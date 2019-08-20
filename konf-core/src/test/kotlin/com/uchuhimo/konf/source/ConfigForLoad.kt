/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the );
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an  BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.uchuhimo.konf.source

import com.uchuhimo.konf.ConfigSpec
import com.uchuhimo.konf.SizeInBytes
import java.io.Serializable
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
import java.time.ZonedDateTime
import java.util.Date
import java.util.SortedMap
import java.util.SortedSet

object ConfigForLoad : ConfigSpec("level1.level2") {
    val empty by required<Int?>()
    val literalEmpty by required<Int?>()
    val present by required<Int?>()

    val boolean by required<Boolean>()

    val int by required<Int>()
    val short by required<Short>()
    val byte by required<Byte>()
    val bigInteger by required<BigInteger>()
    val long by required<Long>()

    val double by required<Double>()
    val float by required<Float>()
    val bigDecimal by required<BigDecimal>()

    val char by required<Char>()

    val string by required<String>()
    val offsetTime by required<OffsetTime>()
    val offsetDateTime by required<OffsetDateTime>()
    val zonedDateTime by required<ZonedDateTime>()
    val localDate by required<LocalDate>()
    val localTime by required<LocalTime>()
    val localDateTime by required<LocalDateTime>()
    val date by required<Date>()
    val year by required<Year>()
    val yearMonth by required<YearMonth>()
    val instant by required<Instant>()
    val duration by required<Duration>()
    val simpleDuration by required<Duration>()
    val size by required<SizeInBytes>()

    val enum by required<EnumForLoad>()

    // array items
    val booleanArray by required<BooleanArray>("array.boolean")
    val byteArray by required<ByteArray>("array.byte")
    val shortArray by required<ShortArray>("array.short")
    val intArray by required<IntArray>("array.int")
    val longArray by required<LongArray>("array.long")
    val floatArray by required<FloatArray>("array.float")
    val doubleArray by required<DoubleArray>("array.double")
    val charArray by required<CharArray>("array.char")

    // object array item
    val booleanObjectArray by required<Array<Boolean>>("array.object.boolean")
    val intObjectArray by required<Array<Int>>("array.object.int")
    val stringArray by required<Array<String>>("array.object.string")
    val enumArray by required<Array<EnumForLoad>>("array.object.enum")

    val list by required<List<Int>>()
    val mutableList by required<List<Int>>()
    val listOfList by required<List<List<Int>>>()
    val set by required<Set<Int>>()
    val sortedSet by required<SortedSet<Int>>()

    val map by required<Map<String, Int>>()
    val sortedMap by required<SortedMap<String, Int>>()
    val listOfMap by required<List<Map<String, Int>>>()

    val nested by required<Array<List<Set<Map<String, Int>>>>>()

    val pair by required<Pair<Int, Int>>()

    val clazz by required<ClassForLoad>()
}

enum class EnumForLoad {
    LABEL1, LABEL2, LABEL3
}

data class ClassForLoad(
    val empty: Int?,
    val literalEmpty: Int?,
    val present: Int?,
    val boolean: Boolean,
    val int: Int,
    val short: Short,
    val byte: Byte,
    val bigInteger: BigInteger,
    val long: Long,
    val double: Double,
    val float: Float,
    val bigDecimal: BigDecimal,
    val char: Char,
    val string: String,
    val offsetTime: OffsetTime,
    val offsetDateTime: OffsetDateTime,
    val zonedDateTime: ZonedDateTime,
    val localDate: LocalDate,
    val localTime: LocalTime,
    val localDateTime: LocalDateTime,
    val date: Date,
    val year: Year,
    val yearMonth: YearMonth,
    val instant: Instant,
    val duration: Duration,
    val simpleDuration: Duration,
    val size: SizeInBytes,
    val enum: EnumForLoad,
    val booleanArray: BooleanArray,
    val nested: Array<List<Set<Map<String, Int>>>>
) : Serializable
