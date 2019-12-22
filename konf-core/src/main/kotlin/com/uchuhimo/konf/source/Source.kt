/*
 * Copyright 2017-2019 the original author or authors.
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
import com.fasterxml.jackson.databind.node.NullNode as JacksonNullNode
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
import com.uchuhimo.konf.ListNode
import com.uchuhimo.konf.MapNode
import com.uchuhimo.konf.MergedMap
import com.uchuhimo.konf.NullNode
import com.uchuhimo.konf.Path
import com.uchuhimo.konf.SizeInBytes
import com.uchuhimo.konf.TreeNode
import com.uchuhimo.konf.ValueNode
import com.uchuhimo.konf.annotation.JavaApi
import com.uchuhimo.konf.source.base.ListStringNode
import com.uchuhimo.konf.source.base.toHierarchical
import com.uchuhimo.konf.toPath
import com.uchuhimo.konf.toTree
import com.uchuhimo.konf.toValue
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
import java.util.ArrayDeque
import java.util.Collections
import java.util.Date
import java.util.Queue
import java.util.SortedMap
import java.util.SortedSet
import java.util.TreeMap
import java.util.TreeSet
import java.util.regex.Pattern
import kotlin.Byte
import kotlin.Char
import kotlin.Double
import kotlin.Float
import kotlin.Int
import kotlin.Long
import kotlin.Short
import kotlin.String
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.starProjectedType
import org.apache.commons.text.StringSubstitutor
import org.apache.commons.text.lookup.StringLookup
import org.apache.commons.text.lookup.StringLookupFactory

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
interface Source {
    /**
     * Description of this source.
     */
    val description: String
        get() = this.info.map { (name, value) ->
            "$name: $value"
        }.joinToString(separator = ", ", prefix = "[", postfix = "]")

    /**
     * Information about this source.
     *
     * Info is in form of key-value pairs.
     */
    val info: SourceInfo

    /**
     * a tree node that represents internal structure of this source.
     */
    val tree: TreeNode

    /**
     * Feature flags in this source.
     */
    val features: Map<Feature, Boolean> get() = emptyMap()

    /**
     * Whether this source contains value(s) in specified path or not.
     *
     * @param path item path
     * @return `true` if this source contains value(s) in specified path, `false` otherwise
     */
    operator fun contains(path: Path): Boolean = path in tree

    /**
     * Returns sub-source in specified path if this source contains value(s) in specified path,
     * `null` otherwise.
     *
     * @param path item path
     * @return sub-source in specified path if this source contains value(s) in specified path,
     * `null` otherwise
     */
    fun getOrNull(path: Path): Source? {
        if (path.isEmpty()) {
            return this
        } else {
            return tree.getOrNull(path)?.let {
                Source(info = info, tree = it, features = features)
            }
        }
    }

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
            var prefixedTree = tree
            for (key in prefix.asReversed()) {
                prefixedTree = ContainerNode(mutableMapOf(key to prefixedTree))
            }
            Source(
                info = this@Source.info,
                tree = prefixedTree,
                features = features
            )
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
    fun withFallback(fallback: Source): Source = object : Source {
        override val info: SourceInfo = SourceInfo(
            "facade" to this@Source.description,
            "fallback" to fallback.description
        )

        override val tree: TreeNode = this@Source.tree.withFallback(fallback.tree)

        override val features: Map<Feature, Boolean>
            get() = MergedMap(
                Collections.unmodifiableMap(fallback.features),
                Collections.unmodifiableMap(this@Source.features))
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
     * Return a source that substitutes path variables within all strings by values.
     *
     * See [StringSubstitutor](https://commons.apache.org/proper/commons-text/apidocs/org/apache/commons/text/StringSubstitutor.html)
     * for detailed substitution rules. An exception is when the string is in reference format like `${path}`,
     * the whole node will be replace by a reference to the sub-tree in the specified path.
     *
     * @param errorWhenUndefined whether throw exception when this source contains undefined path variables
     * @return a source that substitutes path variables within all strings by values
     * @throws UndefinedPathVariableException
     */
    fun substituted(errorWhenUndefined: Boolean = true): Source =
        Source(info, tree.substituted(this, errorWhenUndefined), features)

    /**
     * Returns a new source that enables the specified feature.
     *
     * @param feature the specified feature
     * @return a new source
     */
    fun enabled(feature: Feature): Source = Source(
        info,
        tree,
        MergedMap(Collections.unmodifiableMap(features), mutableMapOf(feature to true))
    )

    /**
     * Returns a new source that disables the specified feature.
     *
     * @param feature the specified feature
     * @return a new source
     */
    fun disabled(feature: Feature): Source = Source(
        info,
        tree,
        MergedMap(Collections.unmodifiableMap(features), mutableMapOf(feature to false))
    )

    /**
     * Check whether the specified feature is enabled or not.
     *
     * @param feature the specified feature
     * @return whether the specified feature is enabled or not
     */
    fun isEnabled(feature: Feature): Boolean = features[feature] ?: feature.enabledByDefault

    companion object {
        operator fun invoke(
            info: SourceInfo = SourceInfo(),
            tree: TreeNode = ContainerNode.empty(),
            features: Map<Feature, Boolean> = emptyMap()
        ): Source {
            return BaseSource(info, tree, features)
        }

        /**
         * Returns default providers.
         *
         * It is a fluent API for default providers.
         */
        val from = DefaultProviders

        /**
         * Returns default providers.
         *
         * It is a fluent API for default providers.
         *
         * @return default providers.
         */
        @JavaApi
        @JvmStatic
        fun from() = from
    }
}

/**
 * Returns a value casted from source.
 *
 * @return a value casted from source
 */
inline fun <reified T> Source.toValue(): T {
    return Config().withSource(this).toValue()
}

private val singleVariablePattern = Pattern.compile("^\\$\\{(.+)}$")

private fun TreeNode.substituted(
    source: Source,
    errorWhenUndefined: Boolean,
    lookup: TreeLookup = TreeLookup(this, source, errorWhenUndefined)
): TreeNode {
    when (this) {
        is NullNode -> return this
        is ValueNode -> {
            if (this is SubstitutableNode && value is String) {
                val text = (if (substituted) originalValue else value) as String
                val matcher = singleVariablePattern.matcher(text.trim())
                if (matcher.find()) {
                    val matchedValue = matcher.group(1)
                    try {
                        val resolvedValue = lookup.replace(matchedValue)
                        val node = lookup.root.getOrNull(resolvedValue)
                        if (node != null) {
                            return node.substituted(source, true, lookup)
                        }
                    } catch (_: Exception) {
                    }
                }
                try {
                    return substitute(lookup.replace(text))
                } catch (_: IllegalArgumentException) {
                    throw UndefinedPathVariableException(source, text)
                }
            } else {
                return this
            }
        }
        is ListNode -> {
            return withList(list.map { it.substituted(source, errorWhenUndefined, lookup) })
        }
        is MapNode -> {
            return withMap(children.mapValues { (_, child) ->
                child.substituted(source, errorWhenUndefined, lookup)
            })
        }
        else -> throw UnsupportedNodeTypeException(source, this)
    }
}

class TreeLookup(val root: TreeNode, val source: Source, errorWhenUndefined: Boolean) : StringLookup {
    val substitutor: StringSubstitutor = StringSubstitutor(
        StringLookupFactory.INSTANCE.interpolatorStringLookup(this)).apply {
        isEnableSubstitutionInVariables = true
        isEnableUndefinedVariableException = errorWhenUndefined
    }

    override fun lookup(key: String): String? {
        val node = root.getOrNull(key)
        if (node != null && node is ValueNode) {
            val value = node.asValueOf(source, String::class.java) as String
            return substitutor.replace(value)
        } else {
            return null
        }
    }

    fun replace(text: String): String {
        return substitutor.replace(text)
    }
}

open class BaseSource(
    override val info: SourceInfo = SourceInfo(),
    override val tree: TreeNode = ContainerNode.empty(),
    override val features: Map<Feature, Boolean> = emptyMap()
) : Source

/**
 * Information of source for debugging.
 */
class SourceInfo(
    private val info: MutableMap<String, String> = mutableMapOf()
) : MutableMap<String, String> by info {
    constructor(vararg pairs: Pair<String, String>) : this(mutableMapOf(*pairs))

    fun with(vararg pairs: Pair<String, String>): SourceInfo {
        return SourceInfo(MergedMap(fallback = this, facade = mutableMapOf(*pairs)))
    }

    fun with(sourceInfo: SourceInfo): SourceInfo {
        return SourceInfo(MergedMap(fallback = this, facade = sourceInfo.toMutableMap()))
    }
}

inline fun <reified T> Source.asValue(): T {
    return tree.asValueOf(this, T::class.java) as T
}

fun TreeNode.asValueOf(source: Source, type: Class<*>): Any {
    return castOrNull(source, type)
        ?: throw WrongTypeException(
            if (this is ValueNode) "${this.value} in ${source.description}"
            else "$this in ${source.description}",
            if (this is ValueNode) this.value::class.java.simpleName else "Unknown",
            type.simpleName
        )
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
        val uniformPath = if (
            source.isEnabled(Feature.LOAD_KEYS_CASE_INSENSITIVELY) ||
            this.isEnabled(Feature.LOAD_KEYS_CASE_INSENSITIVELY)) {
            path.map { it.toLowerCase() }
        } else {
            path
        }
        val itemNode = source.tree.getOrNull(uniformPath)
        if (itemNode != null && !itemNode.isPlaceHolderNode()) {
            if (item.nullable &&
                ((itemNode is NullNode) ||
                    (itemNode is ValueNode && itemNode.value == "null"))) {
                rawSet(item, null)
            } else {
                rawSet(item, itemNode.toValue(source, item.type, mapper))
            }
            return true
        } else {
            return false
        }
    } catch (cause: SourceException) {
        throw LoadException(path, cause)
    }
}

internal fun load(config: Config, source: Source): Source {
    val substitutedSource = if (!config.isEnabled(Feature.SUBSTITUTE_SOURCE_BEFORE_LOADED) ||
        !source.isEnabled(Feature.SUBSTITUTE_SOURCE_BEFORE_LOADED)) {
        source
    } else {
        source.substituted()
    }
    config.lock {
        for (item in config) {
            config.loadItem(item, config.pathOf(item), substitutedSource)
        }
        if (substitutedSource.isEnabled(Feature.FAIL_ON_UNKNOWN_PATH) ||
            config.isEnabled(Feature.FAIL_ON_UNKNOWN_PATH)) {
            val treeFromSource = substitutedSource.tree
            val treeFromConfig = config.toTree()
            val diffTree = treeFromSource - treeFromConfig
            if (diffTree != EmptyNode) {
                val unknownPaths = diffTree.paths
                throw UnknownPathsException(substitutedSource, unknownPaths)
            }
        }
    }
    return substitutedSource
}

private inline fun <reified T> TreeNode.cast(source: Source): T {
    if (this !is ValueNode) {
        throw WrongTypeException("$this in ${source.description}", this::class.java.simpleName, T::class.java.simpleName)
    }
    if (T::class.java.isInstance(value)) {
        return value as T
    } else {
        throw WrongTypeException("$value in ${source.description}", value::class.java.simpleName, T::class.java.simpleName)
    }
}

internal fun stringToBoolean(value: String): Boolean {
    return when {
        value.toLowerCase() == "true" -> true
        value.toLowerCase() == "false" -> false
        else -> throw ParseException("$value cannot be parsed to a boolean")
    }
}

internal fun shortToByte(value: Short): Byte {
    if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
        throw ParseException("$value cannot be parsed to a byte")
    }
    return value.toByte()
}

internal fun intToShort(value: Int): Short {
    if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
        throw ParseException("$value cannot be parsed to a short")
    }
    return value.toShort()
}

internal fun longToInt(value: Long): Int {
    if (value < Int.MIN_VALUE || value > Int.MAX_VALUE) {
        throw ParseException("$value cannot be parsed to an int")
    }
    return value.toInt()
}

internal fun stringToChar(value: String): Char {
    if (value.length != 1) {
        throw ParseException("$value cannot be parsed to a char")
    }
    return value[0]
}

private inline fun <T> String.tryParse(block: (String) -> T): T {
    try {
        return block(this)
    } catch (cause: DateTimeParseException) {
        throw ParseException("fail to parse \"$this\" as data time", cause)
    }
}

internal fun stringToDate(value: String): Date {
    return try {
        Date.from(value.tryParse { Instant.parse(it) })
    } catch (e: ParseException) {
        try {
            Date.from(value.tryParse {
                LocalDateTime.parse(it)
            }.toInstant(ZoneOffset.UTC))
        } catch (e: ParseException) {
            Date.from(value.tryParse {
                LocalDate.parse(it)
            }.atStartOfDay().toInstant(ZoneOffset.UTC))
        }
    }
}

private fun <In, Out> ((In) -> Out).asPromote(): PromoteFunc<*> {
    return { value, _ ->
        @Suppress("UNCHECKED_CAST")
        this(value as In)
    }
}

private inline fun <reified T> tryParseAsPromote(noinline block: (String) -> T): PromoteFunc<*> {
    return { value, _ ->
        try {
            block(value as String)
        } catch (cause: Exception) {
            if (cause is DateTimeParseException || cause is NumberFormatException) {
                throw ParseException("fail to parse \"$value\" as ${T::class.simpleName}", cause)
            } else {
                throw cause
            }
        }
    }
}

typealias PromoteFunc<Out> = (Any, Source) -> Out

private val promoteMap: MutableMap<KClass<*>, List<Pair<KClass<*>, PromoteFunc<*>>>> = mutableMapOf(
    String::class to listOf(
        Boolean::class to ::stringToBoolean.asPromote(),
        Char::class to ::stringToChar.asPromote(),

        Byte::class to tryParseAsPromote { value: String -> value.toByte() },
        Short::class to tryParseAsPromote { value: String -> value.toShort() },
        Int::class to tryParseAsPromote { value: String -> value.toInt() },
        Long::class to tryParseAsPromote { value: String -> value.toLong() },
        Float::class to tryParseAsPromote { value: String -> value.toFloat() },
        Double::class to tryParseAsPromote { value: String -> value.toDouble() },
        BigInteger::class to tryParseAsPromote { value: String -> value.toBigInteger() },
        BigDecimal::class to tryParseAsPromote { value: String -> value.toBigDecimal() },

        OffsetTime::class to tryParseAsPromote { OffsetTime.parse(it) },
        OffsetDateTime::class to tryParseAsPromote { OffsetDateTime.parse(it) },
        ZonedDateTime::class to tryParseAsPromote { ZonedDateTime.parse(it) },
        LocalDate::class to tryParseAsPromote { LocalDate.parse(it) },
        LocalTime::class to tryParseAsPromote { LocalTime.parse(it) },
        LocalDateTime::class to tryParseAsPromote { LocalDateTime.parse(it) },
        Year::class to tryParseAsPromote { Year.parse(it) },
        YearMonth::class to tryParseAsPromote { YearMonth.parse(it) },
        Instant::class to tryParseAsPromote { Instant.parse(it) },

        Date::class to ::stringToDate.asPromote(),
        Duration::class to String::toDuration.asPromote(),

        SizeInBytes::class to { value: String -> SizeInBytes.parse(value) }.asPromote()
    ),
    Char::class to listOf(
        String::class to { value: Char -> "$value" }.asPromote()
    ),
    Byte::class to listOf(
        Short::class to Byte::toShort.asPromote(),
        Int::class to Byte::toInt.asPromote(),
        Long::class to Byte::toLong.asPromote(),
        Float::class to Byte::toFloat.asPromote(),
        Double::class to Byte::toDouble.asPromote()
    ),
    Short::class to listOf(
        Byte::class to ::shortToByte.asPromote(),
        Int::class to Short::toInt.asPromote(),
        Long::class to Short::toLong.asPromote(),
        Float::class to Short::toFloat.asPromote(),
        Double::class to Short::toDouble.asPromote()
    ),
    Int::class to listOf(
        Short::class to ::intToShort.asPromote(),
        Long::class to Int::toLong.asPromote(),
        Float::class to Int::toFloat.asPromote(),
        Double::class to Int::toDouble.asPromote()
    ),
    Long::class to listOf(
        Int::class to ::longToInt.asPromote(),
        Float::class to Long::toFloat.asPromote(),
        Double::class to Long::toDouble.asPromote(),
        BigInteger::class to { value: Long -> BigInteger.valueOf(value) }.asPromote()
    ),
    Float::class to listOf(
        Double::class to Float::toDouble.asPromote()
    ),
    Double::class to listOf(
        Float::class to Double::toFloat.asPromote(),
        BigDecimal::class to { value: Double -> BigDecimal.valueOf(value) }.asPromote()
    )
)

private val promoteMatchers: MutableList<Pair<(KClass<*>) -> Boolean, List<Pair<KClass<*>, PromoteFunc<*>>>>> = mutableListOf(
    { type: KClass<*> -> type.starProjectedType == Array<Int>::class.starProjectedType } to listOf(
        List::class to { value: Array<*> -> value.asList() }.asPromote(),
        Set::class to { value: Array<*> -> value.asList().toSet() }.asPromote()
    ),
    { type: KClass<*> -> type.isSubclassOf(Set::class) } to listOf(
        List::class to { value: Set<*> -> value.toList() }.asPromote()
    )
)

private fun walkPromoteMap(
    valueType: KClass<*>,
    targetType: KClass<*>,
    tasks: Queue<() -> PromoteFunc<*>?>,
    visitedTypes: MutableSet<KClass<*>>,
    previousPromoteFunc: PromoteFunc<*>? = null
): PromoteFunc<*>? {
    if (valueType in visitedTypes) {
        return null
    }
    visitedTypes.add(valueType)
    var promotedTypes = promoteMap[valueType]
    if (promotedTypes == null) {
        for ((matcher, types) in promoteMatchers) {
            if (matcher(valueType)) {
                promotedTypes = types
                break
            }
        }
    }
    if (promotedTypes == null) {
        return null
    }
    for ((promotedType, promoteFunc) in promotedTypes) {
        val currentPromoteFunc: PromoteFunc<*> = if (previousPromoteFunc != null) {
            { value, source -> promoteFunc(previousPromoteFunc(value, source)!!, source) }
        } else {
            promoteFunc
        }
        if (promotedType == targetType) {
            return currentPromoteFunc
        } else {
            tasks.offer {
                walkPromoteMap(promotedType, targetType, tasks, visitedTypes, currentPromoteFunc)
            }
        }
    }
    return null
}

private fun getPromoteFunc(valueType: KClass<*>, targetType: KClass<*>): PromoteFunc<*>? {
    val tasks = ArrayDeque<() -> PromoteFunc<*>?>()
    tasks.offer {
        walkPromoteMap(valueType, targetType, tasks, mutableSetOf())
    }
    while (tasks.isNotEmpty()) {
        val func = tasks.poll()()
        if (func != null) {
            return func
        }
    }
    return null
}

private fun <T : Any> TreeNode.castOrNull(source: Source, clazz: Class<T>): T? {
    if (this is ValueNode) {
        if (clazz.kotlin.javaObjectType.isInstance(value)) {
            @Suppress("UNCHECKED_CAST")
            return value as T
        } else {
            val promoteFunc = getPromoteFunc(value::class, clazz.kotlin)
            if (promoteFunc != null) {
                @Suppress("UNCHECKED_CAST")
                return promoteFunc(value, source) as T
            } else {
                return null
            }
        }
    } else {
        return null
    }
}

private fun TreeNode.toValue(source: Source, type: JavaType, mapper: ObjectMapper): Any {
    if (this is ValueNode &&
        type == TypeFactory.defaultInstance().constructType(value::class.java)) {
        return value
    }
    when (type) {
        is SimpleType -> {
            val clazz = type.rawClass
            if (type.isEnumType) {
                val valueOfMethod = clazz.getMethod("valueOf", String::class.java)
                val name: String = cast(source)
                try {
                    return valueOfMethod.invoke(null, name)
                } catch (cause: InvocationTargetException) {
                    throw ParseException(
                        "enum type $clazz has no constant with name $name", cause)
                }
            } else {
                val value = castOrNull(source, clazz)
                if (value != null) {
                    return value
                } else {
                    try {
                        return mapper.readValue<Any>(
                            TreeTraversingParser(withoutPlaceHolder().toJsonNode(source), mapper),
                            type
                        )
                    } catch (cause: JsonProcessingException) {
                        throw ObjectMappingException("${this.toHierarchical()} in ${source.description}", clazz, cause)
                    }
                }
            }
        }
        is ArrayType -> {
            val clazz = type.contentType.rawClass
            val list = toListValue(source, type.contentType, mapper)
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
                    else -> throw UnsupportedTypeException(source, clazz)
                }
            }
        }
        is CollectionLikeType -> {
            if (type.isTrueCollectionType) {
                @Suppress("UNCHECKED_CAST")
                return (implOf(type.rawClass).getDeclaredConstructor().newInstance() as MutableCollection<Any>).apply {
                    addAll(toListValue(source, type.contentType, mapper) as List<Any>)
                }
            } else {
                throw UnsupportedTypeException(source, type.rawClass)
            }
        }
        is MapLikeType -> {
            if (type.isTrueMapType) {
                if (type.keyType.rawClass == String::class.java) {
                    @Suppress("UNCHECKED_CAST")
                    return (implOf(type.rawClass).getDeclaredConstructor().newInstance() as MutableMap<String, Any>).apply {
                        putAll(this@toValue.toMap(source).mapValues { (_, value) ->
                            value.toValue(source, type.contentType, mapper)
                        })
                    }
                } else {
                    throw UnsupportedMapKeyException(type.keyType.rawClass)
                }
            } else {
                throw UnsupportedTypeException(source, type.rawClass)
            }
        }
        else -> throw UnsupportedTypeException(source, type.rawClass)
    }
}

private fun TreeNode.toListValue(source: Source, type: JavaType, mapper: ObjectMapper): List<*> {
    return when (this) {
        is ListNode -> list.map { it.toValue(source, type, mapper) }
        else -> throw WrongTypeException("$this in ${source.description}", this::class.java.simpleName, List::class.java.simpleName)
    }
}

private fun TreeNode.toMap(source: Source): Map<String, TreeNode> {
    return when (this) {
        is MapNode -> children
        else -> throw WrongTypeException("$this in ${source.description}", this::class.java.simpleName, Map::class.java.simpleName)
    }
}

private fun TreeNode.toJsonNode(source: Source): JsonNode {
    return when (this) {
        is NullNode -> JacksonNullNode.instance
        is ListStringNode ->
            ArrayNode(
                JsonNodeFactory.instance,
                list.map {
                    it.toJsonNode(source)
                })
        is ValueNode -> {
            when (value) {
                is Boolean -> BooleanNode.valueOf(value as Boolean)
                is Long -> LongNode.valueOf(value as Long)
                is Int -> IntNode.valueOf(value as Int)
                is Short -> ShortNode.valueOf(value as Short)
                is Byte -> ShortNode.valueOf((value as Byte).toShort())
                is BigInteger -> BigIntegerNode.valueOf(value as BigInteger)
                is Double -> DoubleNode.valueOf(value as Double)
                is Float -> FloatNode.valueOf(value as Float)
                is Char -> TextNode.valueOf(value.toString())
                is BigDecimal -> DecimalNode.valueOf(value as BigDecimal)
                is String -> TextNode.valueOf(value as String)
                is OffsetTime -> TextNode.valueOf(value.toString())
                is OffsetDateTime -> TextNode.valueOf(value.toString())
                is ZonedDateTime -> TextNode.valueOf(value.toString())
                is LocalDate -> TextNode.valueOf(value.toString())
                is LocalTime -> TextNode.valueOf(value.toString())
                is LocalDateTime -> TextNode.valueOf(value.toString())
                is Date -> TextNode.valueOf((value as Date).toInstant().toString())
                is Year -> TextNode.valueOf(value.toString())
                is YearMonth -> TextNode.valueOf(value.toString())
                is Instant -> TextNode.valueOf(value.toString())
                is Duration -> TextNode.valueOf(value.toString())
                is SizeInBytes -> LongNode.valueOf((value as SizeInBytes).bytes)
                else -> throw ParseException("fail to cast source ${source.description} to JSON node")
            }
        }
        is ListNode ->
            ArrayNode(
                JsonNodeFactory.instance,
                list.map {
                    it.toJsonNode(source)
                })
        is MapNode -> ObjectNode(
            JsonNodeFactory.instance,
            children.mapValues { (_, value) ->
                value.toJsonNode(source)
            }
        )
        else -> throw ParseException("fail to cast source ${source.description} to JSON node")
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

fun Any.asTree(): TreeNode =
    when (this) {
        is TreeNode -> this
        is Source -> this.tree
        is List<*> ->
            @Suppress("UNCHECKED_CAST")
            (ListSourceNode((this as List<Any>).map { it.asTree() }))
        is Map<*, *> -> {
            if (this.size != 0 && this.keys.toList()[0] !is String && this.keys.toList()[0] !is Int) {
                ValueSourceNode(this)
            } else {
                @Suppress("UNCHECKED_CAST")
                (ContainerNode((this as Map<Any, Any>).map { (key, value) ->
                    key.toString() to value.asTree()
                }.toMap().toMutableMap()))
            }
        }
        else -> ValueSourceNode(this)
    }

fun Any.asSource(type: String = "", info: SourceInfo = SourceInfo()): Source =
    when (this) {
        is Source -> this
        is TreeNode -> Source(info.with("type" to type), this)
        else -> Source(info.with("type" to type), asTree())
    }
