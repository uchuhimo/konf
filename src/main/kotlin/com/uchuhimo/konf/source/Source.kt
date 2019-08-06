/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.uchuhimo.konf.source

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.BigIntegerNode
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.DecimalNode
import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.FloatNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.LongNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.ShortNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.databind.node.TreeTraversingParser
import com.fasterxml.jackson.databind.type.ArrayType
import com.fasterxml.jackson.databind.type.CollectionLikeType
import com.fasterxml.jackson.databind.type.MapLikeType
import com.fasterxml.jackson.databind.type.SimpleType
import com.fasterxml.jackson.databind.type.TypeFactory
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ContainerNode
import com.uchuhimo.konf.EmptyNode
import com.uchuhimo.konf.Feature
import com.uchuhimo.konf.Item
import com.uchuhimo.konf.Path
import com.uchuhimo.konf.SizeInBytes
import com.uchuhimo.konf.TreeNode
import com.uchuhimo.konf.ValueNode
import com.uchuhimo.konf.source.base.StringValueSource
import com.uchuhimo.konf.source.base.ValueSource
import com.uchuhimo.konf.source.json.JsonSource
import com.uchuhimo.konf.toPath
import com.uchuhimo.konf.toTree
import com.uchuhimo.konf.unsupported
import java.lang.reflect.InvocationTargetException
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.Year
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException
import java.util.Date
import java.util.SortedMap
import java.util.SortedSet
import java.util.TreeMap
import java.util.TreeSet
import kotlin.Byte
import kotlin.Char
import kotlin.Double
import kotlin.Float
import kotlin.Int
import kotlin.Long
import kotlin.Short
import kotlin.String

/**
 * Source to provide values for config.
 *
 * When config loads values from source, config will iterate all items in it, and
 * retrieve value with path of each item from source.
 * When source contains single value, a series of `is` operations can be used to
 * judge the actual type of value, and `to` operation can be used to get the value
 * with specified type.
 * When source contains multiple value, `contains` operations can be used to check
 * whether value(s) in specified path is in this source, and `get` operations can be used
 * to retrieve the corresponding sub-source.
 */
interface Source : SourceInfo {
    /**
     * Whether this source contains value(s) in specified path or not.
     *
     * @param path item path
     * @return `true` if this source contains value(s) in specified path, `false` otherwise
     */
    operator fun contains(path: Path): Boolean

    /**
     * Returns sub-source in specified path if this source contains value(s) in specified path,
     * `null` otherwise.
     *
     * @param path item path
     * @return sub-source in specified path if this source contains value(s) in specified path,
     * `null` otherwise
     */
    fun getOrNull(path: Path): Source?

    /**
     * Returns sub-source in specified path.
     *
     * Throws [NoSuchPathException] if there is no value in specified path.
     *
     * @param path item path
     * @return sub-source in specified path
     * @throws NoSuchPathException
     */
    operator fun get(path: Path): Source = getOrNull(path) ?: throw NoSuchPathException(this, path)

    /**
     * Whether this source contains value(s) with specified prefix or not.
     *
     * @param prefix item prefix
     * @return `true` if this source contains value(s) with specified prefix, `false` otherwise
     */
    operator fun contains(prefix: String): Boolean = contains(prefix.toPath())

    /**
     * Returns sub-source in specified path if this source contains value(s) in specified path,
     * `null` otherwise.
     *
     * @param path item path
     * @return sub-source in specified path if this source contains value(s) in specified path,
     * `null` otherwise
     */
    fun getOrNull(path: String): Source? = getOrNull(path.toPath())

    /**
     * Returns sub-source in specified path.
     *
     * Throws [NoSuchPathException] if there is no value in specified path.
     *
     * @param path item path
     * @return sub-source in specified path
     * @throws NoSuchPathException
     */
    operator fun get(path: String): Source = get(path.toPath())

    /**
     * Returns source with specified additional prefix.
     *
     * @param prefix additional prefix
     * @return source with specified additional prefix
     */
    fun withPrefix(prefix: Path): Source {
        return if (prefix.isEmpty()) {
            this
        } else {
            object : Source, SourceInfo by SourceInfo.default() {
                init {
                    addInfo("type", "prefix")
                    addInfo("source", this@Source.description)
                }

                override fun contains(path: Path): Boolean {
                    return if (prefix.size >= path.size) {
                        prefix.subList(0, path.size) == path
                    } else {
                        if (path.subList(0, prefix.size) == prefix) {
                            this@Source.contains(path.subList(prefix.size, path.size))
                        } else {
                            false
                        }
                    }
                }

                override fun getOrNull(path: Path): Source? {
                    return if (prefix.size >= path.size) {
                        if (prefix.subList(0, path.size) == path) {
                            this@Source.withPrefix(prefix.subList(path.size, prefix.size))
                        } else {
                            null
                        }
                    } else {
                        if (path.subList(0, prefix.size) == prefix) {
                            this@Source.getOrNull(path.subList(prefix.size, path.size))
                        } else {
                            null
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns source with specified additional prefix.
     *
     * @param prefix additional prefix
     * @return source with specified additional prefix
     */
    fun withPrefix(prefix: String): Source = withPrefix(prefix.toPath())

    /**
     * Returns a source backing by specified fallback source.
     *
     * When config fails to retrieve values from this source, it will try to retrieve them from
     * fallback source.
     *
     * @param fallback fallback source
     * @return a source backing by specified fallback source
     */
    fun withFallback(fallback: Source): Source = object : Source, SourceInfo by SourceInfo.default() {
        init {
            addInfo("facade", this@Source.description)
            addInfo("fallback", fallback.description)
        }

        override fun contains(path: Path): Boolean =
            this@Source.contains(path) || fallback.contains(path)

        override fun getOrNull(path: Path): Source? =
            this@Source.getOrNull(path) ?: fallback.getOrNull(path)
    }

    /**
     * Returns a source overlapped by the specified facade source.
     *
     * When config fails to retrieve values from the facade source, it will try to retrieve them
     * from this source.
     *
     * @param facade the facade source
     * @return a source overlapped by the specified facade source
     */
    operator fun plus(facade: Source): Source = facade.withFallback(this)

    /**
     * Returns a new source that enables the specified feature.
     *
     * @param feature the specified feature
     * @return a new source
     */
    fun enabled(feature: Feature): Source = FeaturedSource(this, feature, true)

    /**
     * Returns a new source that disables the specified feature.
     *
     * @param feature the specified feature
     * @return a new source
     */
    fun disabled(feature: Feature): Source = FeaturedSource(this, feature, false)

    /**
     * Check whether the specified feature is enabled or not.
     *
     * @param feature the specified feature
     * @return whether the specified feature is enabled or not
     */
    fun isEnabled(feature: Feature): Boolean = feature.enabledByDefault

    /**
     * Whether this source contains a null value or not.
     *
     * @return `true` if this source contains a null value, `false` otherwise
     */
    fun isNull(): Boolean = false

    /**
     * Whether this source contains a list of values or not.
     *
     * @return `true` if this source contains a list of values, `false` otherwise
     */
    fun isList(): Boolean = false

    /**
     * Get a list of sub-sources from this source.
     *
     * @return a list of sub-sources
     */
    fun toList(): List<Source> = unsupported()

    /**
     * Whether this source contains multiple values mapping from corresponding paths or not.
     *
     * @return `true` if this source contains multiple values mapping from corresponding paths,
     * `false` otherwise
     */
    fun isMap(): Boolean = false

    /**
     * Get a map from paths to corresponding values from this source.
     *
     * @return a map from paths to corresponding values
     */
    fun toMap(): Map<String, Source> = unsupported()

    /**
     * Whether this source contains a single value with type [String] or not.
     *
     * @return `true` if this source contains a single value with type [String], `false` otherwise
     */
    fun isText(): Boolean = false

    /**
     * Get a [String] value from this source.
     *
     * @return a [String] value
     */
    fun toText(): String = unsupported()

    /**
     * Whether this source contains a single value with type [Boolean] or not.
     *
     * @return `true` if this source contains a single value with type [Boolean], `false` otherwise
     */
    fun isBoolean(): Boolean = false

    /**
     * Get a [Boolean] value from this source.
     *
     * @return a [Boolean] value
     */
    fun toBoolean(): Boolean = unsupported()

    /**
     * Whether this source contains a single value with type [Long] or not.
     *
     * @return `true` if this source contains a single value with type [Long], `false` otherwise
     */
    fun isLong(): Boolean = false

    /**
     * Get a [Long] value from this source.
     *
     * @return a [Long] value
     */
    fun toLong(): Long = toInt().toLong()

    /**
     * Whether this source contains a single value with type [Double] or not.
     *
     * @return `true` if this source contains a single value with type [Double], `false` otherwise
     */
    fun isDouble(): Boolean = false

    /**
     * Get a [Double] value from this source.
     *
     * @return a [Double] value
     */
    fun toDouble(): Double = unsupported()

    /**
     * Whether this source contains a single value with type [Int] or not.
     *
     * @return `true` if this source contains a single value with type [Int], `false` otherwise
     */
    fun isInt(): Boolean = false

    /**
     * Get a [Int] value from this source.
     *
     * @return a [Int] value
     */
    fun toInt(): Int = unsupported()

    /**
     * Whether this source contains a single value with type [Short] or not.
     *
     * @return `true` if this source contains a single value with type [Short], `false` otherwise
     */
    fun isShort(): Boolean = false

    /**
     * Get a [Short] value from this source.
     *
     * @return a [Short] value
     */
    fun toShort(): Short = toInt().also { value ->
        if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
            throw ParseException("$value is out of range of Short")
        }
    }.toShort()

    /**
     * Whether this source contains a single value with type [Byte] or not.
     *
     * @return `true` if this source contains a single value with type [Byte], `false` otherwise
     */
    fun isByte(): Boolean = false

    /**
     * Get a [Byte] value from this source.
     *
     * @return a [Byte] value
     */
    fun toByte(): Byte = toInt().also { value ->
        if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
            throw ParseException("$value is out of range of Byte")
        }
    }.toByte()

    /**
     * Whether this source contains a single value with type [Float] or not.
     *
     * @return `true` if this source contains a single value with type [Float], `false` otherwise
     */
    fun isFloat(): Boolean = false

    /**
     * Get a [Float] value from this source.
     *
     * @return a [Float] value
     */
    fun toFloat(): Float = toDouble().toFloat()

    /**
     * Whether this source contains a single value with type [Char] or not.
     *
     * @return `true` if this source contains a single value with type [Char], `false` otherwise
     */
    fun isChar(): Boolean = false

    /**
     * Get a [Char] value from this source.
     *
     * @return a [Char] value
     */
    fun toChar(): Char {
        val value = toText()
        if (value.length != 1) {
            throw WrongTypeException(this, "String", "Char")
        }
        return value[0]
    }

    /**
     * Whether this source contains a single value with type [BigInteger] or not.
     *
     * @return `true` if this source contains a single value with type [BigInteger], `false` otherwise
     */
    fun isBigInteger(): Boolean = false

    /**
     * Get a [BigInteger] value from this source.
     *
     * @return a [BigInteger] value
     */
    fun toBigInteger(): BigInteger = BigInteger.valueOf(toLong())

    /**
     * Whether this source contains a single value with type [BigDecimal] or not.
     *
     * @return `true` if this source contains a single value with type [BigDecimal], `false` otherwise
     */
    fun isBigDecimal(): Boolean = false

    /**
     * Get a [BigDecimal] value from this source.
     *
     * @return a [BigDecimal] value
     */
    fun toBigDecimal(): BigDecimal = BigDecimal.valueOf(toDouble())

    private inline fun <T> tryParse(block: () -> T): T {
        try {
            return block()
        } catch (cause: DateTimeParseException) {
            throw ParseException("fail to parse \"${toText()}\" as data time", cause)
        }
    }

    /**
     * Whether this source contains a single value with type [OffsetTime] or not.
     *
     * @return `true` if this source contains a single value with type [OffsetTime], `false` otherwise
     */
    fun isOffsetTime(): Boolean = false

    /**
     * Get a [OffsetTime] value from this source.
     *
     * @return a [OffsetTime] value
     */
    fun toOffsetTime(): OffsetTime = tryParse { OffsetTime.parse(toText()) }

    /**
     * Whether this source contains a single value with type [OffsetDateTime] or not.
     *
     * @return `true` if this source contains a single value with type [OffsetDateTime], `false` otherwise
     */
    fun isOffsetDateTime(): Boolean = false

    /**
     * Get a [OffsetDateTime] value from this source.
     *
     * @return a [OffsetDateTime] value
     */
    fun toOffsetDateTime(): OffsetDateTime = tryParse { OffsetDateTime.parse(toText()) }

    /**
     * Whether this source contains a single value with type [ZonedDateTime] or not.
     *
     * @return `true` if this source contains a single value with type [ZonedDateTime], `false` otherwise
     */
    fun isZonedDateTime(): Boolean = false

    /**
     * Get a [ZonedDateTime] value from this source.
     *
     * @return a [ZonedDateTime] value
     */
    fun toZonedDateTime(): ZonedDateTime = tryParse { ZonedDateTime.parse(toText()) }

    /**
     * Whether this source contains a single value with type [LocalDate] or not.
     *
     * @return `true` if this source contains a single value with type [LocalDate], `false` otherwise
     */
    fun isLocalDate(): Boolean = false

    /**
     * Get a [LocalDate] value from this source.
     *
     * @return a [LocalDate] value
     */
    fun toLocalDate(): LocalDate = tryParse { LocalDate.parse(toText()) }

    /**
     * Whether this source contains a single value with type [LocalTime] or not.
     *
     * @return `true` if this source contains a single value with type [LocalTime], `false` otherwise
     */
    fun isLocalTime(): Boolean = false

    /**
     * Get a [LocalTime] value from this source.
     *
     * @return a [LocalTime] value
     */
    fun toLocalTime(): LocalTime = tryParse { LocalTime.parse(toText()) }

    /**
     * Whether this source contains a single value with type [LocalDateTime] or not.
     *
     * @return `true` if this source contains a single value with type [LocalDateTime], `false` otherwise
     */
    fun isLocalDateTime(): Boolean = false

    /**
     * Get a [LocalDateTime] value from this source.
     *
     * @return a [LocalDateTime] value
     */
    fun toLocalDateTime(): LocalDateTime = tryParse { LocalDateTime.parse(toText()) }

    /**
     * Whether this source contains a single value with type [Date] or not.
     *
     * @return `true` if this source contains a single value with type [Date], `false` otherwise
     */
    fun isDate(): Boolean = false

    /**
     * Get a [Date] value from this source.
     *
     * @return a [Date] value
     */
    fun toDate(): Date {
        return try {
            Date.from(tryParse { Instant.parse(toText()) })
        } catch (e: ParseException) {
            try {
                Date.from(tryParse {
                    LocalDateTime.parse(toText())
                }.toInstant(ZoneOffset.UTC))
            } catch (e: ParseException) {
                Date.from(tryParse {
                    LocalDate.parse(toText())
                }.atStartOfDay().toInstant(ZoneOffset.UTC))
            }
        }
    }

    /**
     * Whether this source contains a single value with type [Year] or not.
     *
     * @return `true` if this source contains a single value with type [Year], `false` otherwise
     */
    fun isYear(): Boolean = false

    /**
     * Get a [Year] value from this source.
     *
     * @return a [Year] value
     */
    fun toYear(): Year = tryParse { Year.parse(toText()) }

    /**
     * Whether this source contains a single value with type [YearMonth] or not.
     *
     * @return `true` if this source contains a single value with type [YearMonth], `false` otherwise
     */
    fun isYearMonth(): Boolean = false

    /**
     * Get a [YearMonth] value from this source.
     *
     * @return a [YearMonth] value
     */
    fun toYearMonth(): YearMonth = tryParse { YearMonth.parse(toText()) }

    /**
     * Whether this source contains a single value with type [Instant] or not.
     *
     * @return `true` if this source contains a single value with type [Instant], `false` otherwise
     */
    fun isInstant(): Boolean = false

    /**
     * Get a [Instant] value from this source.
     *
     * @return a [Instant] value
     */
    fun toInstant(): Instant = tryParse { Instant.parse(toText()) }

    /**
     * Whether this source contains a single value with type [Duration] or not.
     *
     * @return `true` if this source contains a single value with type [Duration], `false` otherwise
     */
    fun isDuration(): Boolean = false

    /**
     * Get a [Duration] value from this source.
     *
     * @return a [Duration] value
     */
    fun toDuration(): Duration = toText().toDuration()

    /**
     * Whether this source contains a single value with type [SizeInBytes] or not.
     *
     * @return `true` if this source contains a single value with type [SizeInBytes], `false` otherwise
     */
    fun isSizeInBytes(): Boolean = false

    /**
     * Get a [SizeInBytes] value from this source.
     *
     * @return a [SizeInBytes] value
     */
    fun toSizeInBytes(): SizeInBytes = SizeInBytes.parse(toText())
}

/**
 * Source with the specified feature enabled/disabled.
 */
class FeaturedSource(
    source: Source,
    private val feature: Feature,
    private val isEnabled: Boolean
) : Source by source {
    override fun isEnabled(feature: Feature): Boolean {
        return if (feature == this.feature) isEnabled else super.isEnabled(feature)
    }
}

internal fun Any?.toCompatibleValue(mapper: ObjectMapper): Any {
    return when (this) {
        is OffsetTime,
        is OffsetDateTime,
        is ZonedDateTime,
        is LocalDate,
        is LocalDateTime,
        is LocalTime,
        is Year,
        is YearMonth,
        is Instant,
        is Duration -> this.toString()
        is Date -> this.toInstant().toString()
        is SizeInBytes -> this.bytes.toString()
        is Enum<*> -> this.name
        is ByteArray -> this.toList()
        is CharArray -> this.toList().map { it.toString() }
        is BooleanArray -> this.toList()
        is IntArray -> this.toList()
        is ShortArray -> this.toList()
        is LongArray -> this.toList()
        is DoubleArray -> this.toList()
        is FloatArray -> this.toList()
        is List<*> -> this.map { it!!.toCompatibleValue(mapper) }
        is Set<*> -> this.map { it!!.toCompatibleValue(mapper) }
        is Array<*> -> this.map { it!!.toCompatibleValue(mapper) }
        is Map<*, *> -> this.mapValues { (_, value) -> value!!.toCompatibleValue(mapper) }
        is Char -> this.toString()
        is String,
        is Boolean,
        is Int,
        is Short,
        is Byte,
        is Long,
        is BigInteger,
        is Double,
        is Float,
        is BigDecimal -> this
        else -> {
            if (this == null) {
                "null"
            } else {
                val result = mapper.convertValue(this, Map::class.java).mapValues { (_, value) ->
                    value.toCompatibleValue(mapper)
                }
                result
            }
        }
    }
}

internal fun Config.loadItem(item: Item<*>, path: Path, source: Source): Boolean {
    try {
        val uniformPath = if (source.isEnabled(Feature.LOAD_KEYS_CASE_INSENSITIVELY)) {
            path.map { it.toLowerCase() }
        } else {
            path
        }
        val itemSource = source.getOrNull(uniformPath)
        return if (itemSource != null) {
            if (item.nullable &&
                (itemSource.isNull() ||
                    (itemSource.isText() && itemSource.toText() == "null"))) {
                rawSet(item, null)
            } else {
                rawSet(item, itemSource.toValue(item.type, mapper))
            }
            true
        } else {
            false
        }
    } catch (cause: SourceException) {
        throw LoadException(path, cause)
    }
}

internal fun load(config: Config, source: Source): Config {
    return config.apply {
        lock {
            for (item in this) {
                loadItem(item, pathOf(item), source)
            }
            if (source.isEnabled(Feature.FAIL_ON_UNKNOWN_PATH) ||
                config.isEnabled(Feature.FAIL_ON_UNKNOWN_PATH)) {
                val treeFromSource = source.toTree()
                check(!treeFromSource.isLeaf())
                val treeFromConfig = config.toTree()
                val diffTree = treeFromSource - treeFromConfig
                if (diffTree != EmptyNode) {
                    val unknownPaths = diffTree.paths
                    throw UnknownPathsException(source, unknownPaths)
                }
            }
        }
    }
}

internal fun Config.loadBy(
    description: String,
    trigger: (
        config: Config,
        load: (source: Source) -> Unit
    ) -> Unit
): Config {
    return withLayer("trigger: $description").apply {
        trigger(this) { source ->
            load(this, source)
        }
    }
}

/**
 * Convert the source to a tree node.
 *
 * @return a tree node
 */
fun Source.toTree(): TreeNode {
    return if (isMap()) {
        ContainerNode(toMap().mapValues { it.value.toTree() })
    } else {
        ValueNode()
    }
}

private fun Source.toValue(type: JavaType, mapper: ObjectMapper): Any {
    if (this is ValueSource &&
        type == TypeFactory.defaultInstance().constructType(value::class.java)) {
        return value
    }
    when (type) {
        is SimpleType -> {
            val clazz = type.rawClass
            if (type.isEnumType) {
                val valueOfMethod = clazz.getMethod("valueOf", String::class.java)
                val name = toText()
                try {
                    return valueOfMethod.invoke(null, name)
                } catch (cause: InvocationTargetException) {
                    throw ParseException(
                        "enum type $clazz has no constant with name $name", cause)
                }
            } else {
                return when (clazz) {
                    Boolean::class.javaObjectType, Boolean::class.java -> toBoolean()
                    Int::class.javaObjectType, Int::class.java -> toInt()
                    Short::class.javaObjectType, Short::class.java -> toShort()
                    Byte::class.javaObjectType, Byte::class.java -> toByte()
                    Long::class.javaObjectType, Long::class.java -> toLong()
                    BigInteger::class.java -> toBigInteger()
                    Double::class.javaObjectType, Double::class.java -> toDouble()
                    Float::class.javaObjectType, Float::class.java -> toFloat()
                    BigDecimal::class.java -> toBigDecimal()
                    Char::class.javaObjectType, Char::class.java -> toChar()
                    String::class.java -> toText()
                    OffsetTime::class.java -> toOffsetTime()
                    OffsetDateTime::class.java -> toOffsetDateTime()
                    ZonedDateTime::class.java -> toZonedDateTime()
                    LocalDate::class.java -> toLocalDate()
                    LocalTime::class.java -> toLocalTime()
                    LocalDateTime::class.java -> toLocalDateTime()
                    Date::class.java -> toDate()
                    Year::class.java -> toYear()
                    YearMonth::class.java -> toYearMonth()
                    Instant::class.java -> toInstant()
                    Duration::class.java -> toDuration()
                    SizeInBytes::class.java -> toSizeInBytes()
                    else -> {
                        if (this is ValueSource && clazz == value.javaClass) {
                            value
                        } else {
                            try {
                                mapper.readValue<Any>(
                                    TreeTraversingParser(toJsonNode(), mapper),
                                    type)
                            } catch (cause: JsonProcessingException) {
                                throw ObjectMappingException(this, clazz, cause)
                            }
                        }
                    }
                }
            }
        }
        is ArrayType -> {
            val clazz = type.contentType.rawClass
            val list = toListValue(type.contentType, mapper)
            if (!clazz.isPrimitive) {
                val array = java.lang.reflect.Array.newInstance(clazz, list.size) as Array<*>
                @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                return (list as java.util.Collection<*>).toArray(array)
            } else {
                @Suppress("UNCHECKED_CAST")
                return when (clazz) {
                    Boolean::class.java -> (list as List<Boolean>).toBooleanArray()
                    Int::class.java -> (list as List<Int>).toIntArray()
                    Short::class.java -> (list as List<Short>).toShortArray()
                    Byte::class.java -> (list as List<Byte>).toByteArray()
                    Long::class.java -> (list as List<Long>).toLongArray()
                    Double::class.java -> (list as List<Double>).toDoubleArray()
                    Float::class.java -> (list as List<Float>).toFloatArray()
                    Char::class.java -> (list as List<Char>).toCharArray()
                    else -> throw UnsupportedTypeException(this, clazz)
                }
            }
        }
        is CollectionLikeType -> {
            if (type.isTrueCollectionType) {
                @Suppress("UNCHECKED_CAST")
                return (implOf(type.rawClass).getDeclaredConstructor().newInstance() as MutableCollection<Any>).apply {
                    addAll(toListValue(type.contentType, mapper))
                }
            } else {
                throw UnsupportedTypeException(this, type.rawClass)
            }
        }
        is MapLikeType -> {
            if (type.isTrueMapType) {
                if (type.keyType.rawClass == String::class.java) {
                    @Suppress("UNCHECKED_CAST")
                    return (implOf(type.rawClass).getDeclaredConstructor().newInstance() as MutableMap<String, Any>).apply {
                        putAll(this@toValue.toMap().mapValues { (_, value) ->
                            value.toValue(type.contentType, mapper)
                        })
                    }
                } else {
                    throw UnsupportedMapKeyException(type.keyType.rawClass)
                }
            } else {
                throw UnsupportedTypeException(this, type.rawClass)
            }
        }
        else -> throw UnsupportedTypeException(this, type.rawClass)
    }
}

private fun Source.toListValue(type: JavaType, mapper: ObjectMapper) =
    toList().map { it.toValue(type, mapper) }

private fun Source.toJsonNode(canBeList: Boolean = true): JsonNode {
    if (this is JsonSource) {
        return this.node
    } else {
        return when {
            isNull() -> NullNode.instance
            isList() && canBeList ->
                if (this is StringValueSource && !isRegularList()) {
                    toJsonNode(canBeList = false)
                } else {
                    ArrayNode(
                        JsonNodeFactory.instance,
                        toList().map {
                            it.toJsonNode(canBeList = canBeList)
                        })
                }
            isMap() -> ObjectNode(
                JsonNodeFactory.instance,
                toMap().mapValues { (_, value) ->
                    value.toJsonNode(canBeList = canBeList)
                }
            )
            isBoolean() -> BooleanNode.valueOf(toBoolean())
            isLong() -> LongNode.valueOf(toLong())
            isInt() -> IntNode.valueOf(toInt())
            isShort() -> ShortNode.valueOf(toShort())
            isByte() -> ShortNode.valueOf(toByte().toShort())
            isBigInteger() -> BigIntegerNode.valueOf(toBigInteger())
            isDouble() -> DoubleNode.valueOf(toDouble())
            isFloat() -> FloatNode.valueOf(toFloat())
            isChar() -> TextNode.valueOf(toChar().toString())
            isBigDecimal() -> DecimalNode.valueOf(toBigDecimal())
            isText() -> TextNode.valueOf(toText())
            isOffsetTime() -> TextNode.valueOf(toOffsetTime().toString())
            isOffsetDateTime() -> TextNode.valueOf(toOffsetDateTime().toString())
            isZonedDateTime() -> TextNode.valueOf(toZonedDateTime().toString())
            isLocalDate() -> TextNode.valueOf(toLocalDate().toString())
            isLocalTime() -> TextNode.valueOf(toLocalTime().toString())
            isLocalDateTime() -> TextNode.valueOf(toLocalDateTime().toString())
            isDate() -> TextNode.valueOf(toDate().toInstant().toString())
            isYear() -> TextNode.valueOf(toYear().toString())
            isYearMonth() -> TextNode.valueOf(toYearMonth().toString())
            isInstant() -> TextNode.valueOf(toInstant().toString())
            isDuration() -> TextNode.valueOf(toDuration().toString())
            isSizeInBytes() -> LongNode.valueOf(toSizeInBytes().bytes)
            else -> throw ParseException("fail to cast source $description to JSON node")
        }
    }
}

private fun implOf(clazz: Class<*>): Class<*> =
    when (clazz) {
        List::class.java -> ArrayList::class.java
        Set::class.java -> HashSet::class.java
        SortedSet::class.java -> TreeSet::class.java
        Map::class.java -> HashMap::class.java
        SortedMap::class.java -> TreeMap::class.java
        else -> clazz
    }

object EmptySource : Source {
    override fun contains(path: Path) = false

    override fun getOrNull(path: Path): Source? = null

    override val context = mutableMapOf<String,String>()

    override fun addContext(name: String, value: String) {
        context[name] = value
    }

    override val info = mutableMapOf<String,String>()

    override fun addInfo(name: String, value: String) {
        info[name] = value
    }

}