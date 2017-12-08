/*
 * Copyright 2017 the original author or authors.
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

package com.uchuhimo.konf.source.base

import com.uchuhimo.konf.Path
import com.uchuhimo.konf.SizeInBytes
import com.uchuhimo.konf.notEmptyOr
import com.uchuhimo.konf.source.NoSuchPathException
import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.SourceInfo
import com.uchuhimo.konf.source.WrongTypeException
import com.uchuhimo.konf.source.toDescription
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
import java.util.Date

/**
 * Source from a single value.
 */
open class ValueSource(
        val value: Any,
        type: String = "",
        context: Map<String, String> = mapOf()
) : Source, SourceInfo by SourceInfo.with(context) {
    init {
        @Suppress("LeakingThis")
        addInfo("type", type.notEmptyOr("value"))
    }

    override fun contains(path: Path): Boolean = path.isEmpty()

    override fun getOrNull(path: Path): Source? {
        if (path.isEmpty()) {
            return this
        } else {
            throw NoSuchPathException(this, path)
        }
    }

    protected inline fun <reified T> cast(): T {
        if (T::class.java.isInstance(value)) {
            return value as T
        } else {
            throw WrongTypeException(this, value::class.java.simpleName, T::class.java.simpleName)
        }
    }

    open fun Any.castToSource(context: Map<String, String>): Source = asSource(context = context)

    override fun isList(): Boolean = value is List<*>

    override fun toList(): List<Source> = cast<List<Any>>().map {
        it.castToSource(context).apply { addInfo("inList", this@ValueSource.info.toDescription()) }
    }

    override fun isText(): Boolean = value is String

    override fun toText(): String = cast()

    override fun isBoolean(): Boolean = value is Boolean

    override fun toBoolean(): Boolean = cast()

    override fun isLong(): Boolean = value is Long

    override fun toLong(): Long {
        return try {
            cast()
        } catch (e: WrongTypeException) {
            super.toLong()
        }
    }

    override fun isDouble(): Boolean = value is Double

    override fun toDouble(): Double {
        return try {
            cast()
        } catch (e: WrongTypeException) {
            toLong().toDouble()
        }
    }

    override fun isInt(): Boolean = value is Int

    override fun toInt(): Int = cast()

    override fun isShort(): Boolean = value is Short

    override fun toShort(): Short {
        return try {
            cast()
        } catch (e: WrongTypeException) {
            super.toShort()
        }
    }

    override fun isByte(): Boolean = value is Byte

    override fun toByte(): Byte {
        return try {
            cast()
        } catch (e: WrongTypeException) {
            super.toByte()
        }
    }

    override fun isFloat(): Boolean = value is Float

    override fun toFloat(): Float {
        return try {
            cast()
        } catch (e: WrongTypeException) {
            super.toFloat()
        }
    }

    override fun isChar(): Boolean = value is Char

    override fun toChar(): Char {
        return try {
            cast()
        } catch (e: WrongTypeException) {
            super.toChar()
        }
    }

    override fun isBigInteger(): Boolean = value is BigInteger

    override fun toBigInteger(): BigInteger {
        return try {
            cast()
        } catch (e: WrongTypeException) {
            super.toBigInteger()
        }
    }

    override fun isBigDecimal(): Boolean = value is BigDecimal

    override fun toBigDecimal(): BigDecimal {
        return try {
            cast()
        } catch (e: WrongTypeException) {
            super.toBigDecimal()
        }
    }

    override fun isOffsetTime(): Boolean = value is OffsetTime

    override fun toOffsetTime(): OffsetTime {
        return try {
            cast()
        } catch (e: WrongTypeException) {
            super.toOffsetTime()
        }
    }

    override fun isOffsetDateTime(): Boolean = value is OffsetDateTime

    override fun toOffsetDateTime(): OffsetDateTime {
        return try {
            cast()
        } catch (e: WrongTypeException) {
            super.toOffsetDateTime()
        }
    }

    override fun isZonedDateTime(): Boolean = value is ZonedDateTime

    override fun toZonedDateTime(): ZonedDateTime {
        return try {
            cast()
        } catch (e: WrongTypeException) {
            super.toZonedDateTime()
        }
    }

    override fun isLocalDate(): Boolean = value is LocalDate

    override fun toLocalDate(): LocalDate {
        return try {
            cast()
        } catch (e: WrongTypeException) {
            try {
                LocalDateTime.ofInstant(cast<Date>().toInstant(), ZoneOffset.UTC).toLocalDate()
            } catch (e: WrongTypeException) {
                super.toLocalDate()
            }
        }
    }

    override fun isLocalTime(): Boolean = value is LocalTime

    override fun toLocalTime(): LocalTime {
        return try {
            cast()
        } catch (e: WrongTypeException) {
            super.toLocalTime()
        }
    }

    override fun isLocalDateTime(): Boolean = value is LocalDateTime

    override fun toLocalDateTime(): LocalDateTime {
        return try {
            cast()
        } catch (e: WrongTypeException) {
            try {
                LocalDateTime.ofInstant(cast<Date>().toInstant(), ZoneOffset.UTC)
            } catch (e: WrongTypeException) {
                super.toLocalDateTime()
            }
        }
    }

    override fun isDate(): Boolean = value is Date

    override fun toDate(): Date {
        return try {
            cast()
        } catch (e: WrongTypeException) {
            super.toDate()
        }
    }

    override fun isYear(): Boolean = value is Year

    override fun toYear(): Year {
        return try {
            cast()
        } catch (e: WrongTypeException) {
            super.toYear()
        }
    }

    override fun isYearMonth(): Boolean = value is YearMonth

    override fun toYearMonth(): YearMonth {
        return try {
            cast()
        } catch (e: WrongTypeException) {
            super.toYearMonth()
        }
    }

    override fun isInstant(): Boolean = value is Instant

    override fun toInstant(): Instant {
        return try {
            cast()
        } catch (e: WrongTypeException) {
            try {
                cast<Date>().toInstant()
            } catch (e: WrongTypeException) {
                super.toInstant()
            }
        }
    }

    override fun isDuration(): Boolean = value is Duration

    override fun toDuration(): Duration {
        return try {
            cast()
        } catch (e: WrongTypeException) {
            super.toDuration()
        }
    }

    override fun isSizeInBytes(): Boolean = value is SizeInBytes

    override fun toSizeInBytes(): SizeInBytes {
        return try {
            cast()
        } catch (e: WrongTypeException) {
            super.toSizeInBytes()
        }
    }
}

fun Any.asSource(type: String = "", context: Map<String, String> = mapOf()): Source =
        when {
            this is Source -> this
            this is Map<*, *> ->
                // assume that only `Map<String, Any>` is provided,
                // key type mismatch will be detected when loaded into Config
                @Suppress("UNCHECKED_CAST")
                MapSource(this as Map<String, Any>, type, context)
            else -> ValueSource(this, type, context)
        }
