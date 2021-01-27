/*
 * Copyright 2017-2021 the original author or authors.
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

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.toSizeInBytes
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.subject.SubjectSpek
import org.jetbrains.spek.subject.itBehavesLike
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.math.BigDecimal
import java.math.BigInteger
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

object SourceLoadSpec : SubjectSpek<Config>({

    subject {
        Config {
            addSpec(ConfigForLoad)
        }.from.map.kv(loadContent)
    }

    itBehavesLike(SourceLoadBaseSpec)
})

object SourceReloadSpec : SubjectSpek<Config>({

    subject {
        val config = Config {
            addSpec(ConfigForLoad)
        }.from.map.kv(loadContent)
        Config {
            addSpec(ConfigForLoad)
        }.from.map.kv(config.toMap())
    }

    itBehavesLike(SourceLoadBaseSpec)
})

object SourceReloadFromDiskSpec : SubjectSpek<Config>({

    subject {
        val config = Config {
            addSpec(ConfigForLoad)
        }.from.map.kv(loadContent)
        val map = config.toMap()
        val newMap = createTempFile().run {
            ObjectOutputStream(outputStream()).use {
                it.writeObject(map)
            }
            ObjectInputStream(inputStream()).use {
                @Suppress("UNCHECKED_CAST")
                it.readObject() as Map<String, Any>
            }
        }
        Config {
            addSpec(ConfigForLoad)
        }.from.map.kv(newMap)
    }

    itBehavesLike(SourceLoadBaseSpec)
})

object KVSourceFromDefaultProvidersSpec : SubjectSpek<Config>({

    subject {
        Config {
            addSpec(ConfigForLoad)
        }.withSource(Source.from.map.kv(loadContent))
    }

    itBehavesLike(SourceLoadBaseSpec)
})

private val loadContent = mapOf<String, Any>(
    "empty" to "null",
    "literalEmpty" to "null",
    "present" to 1,

    "boolean" to false,

    "int" to 1,
    "short" to 2.toShort(),
    "byte" to 3.toByte(),
    "bigInteger" to BigInteger.valueOf(4),
    "long" to 4L,

    "double" to 1.5,
    "float" to -1.5f,
    "bigDecimal" to BigDecimal.valueOf(1.5),

    "char" to 'a',

    "string" to "string",
    "offsetTime" to OffsetTime.parse("10:15:30+01:00"),
    "offsetDateTime" to OffsetDateTime.parse("2007-12-03T10:15:30+01:00"),
    "zonedDateTime" to ZonedDateTime.parse("2007-12-03T10:15:30+01:00[Europe/Paris]"),
    "localDate" to LocalDate.parse("2007-12-03"),
    "localTime" to LocalTime.parse("10:15:30"),
    "localDateTime" to LocalDateTime.parse("2007-12-03T10:15:30"),
    "date" to Date.from(Instant.parse("2007-12-03T10:15:30Z")),
    "year" to Year.parse("2007"),
    "yearMonth" to YearMonth.parse("2007-12"),
    "instant" to Instant.parse("2007-12-03T10:15:30.00Z"),
    "duration" to "P2DT3H4M".toDuration(),
    "simpleDuration" to "200millis".toDuration(),
    "size" to "10k".toSizeInBytes(),

    "enum" to "LABEL2",

    "array.boolean" to listOf(true, false),
    "array.byte" to listOf<Byte>(1, 2, 3),
    "array.short" to listOf<Short>(1, 2, 3),
    "array.int" to listOf(1, 2, 3),
    "array.long" to listOf(4L, 5L, 6L),
    "array.float" to listOf(-1.0F, 0.0F, 1.0F),
    "array.double" to listOf(-1.0, 0.0, 1.0),
    "array.char" to listOf('a', 'b', 'c'),

    "array.object.boolean" to listOf(true, false),
    "array.object.int" to listOf(1, 2, 3),
    "array.object.string" to listOf("one", "two", "three"),
    "array.object.enum" to listOf("LABEL1", "LABEL2", "LABEL3"),

    "list" to listOf(1, 2, 3),
    "mutableList" to listOf(1, 2, 3),
    "listOfList" to listOf(listOf(1, 2), listOf(3, 4)),
    "set" to listOf(1, 2, 1),
    "sortedSet" to listOf(2, 1, 1, 3),

    "map" to mapOf(
        "a" to 1,
        "b" to 2,
        "c" to 3
    ),
    "intMap" to mapOf(
        1 to "a",
        2 to "b",
        3 to "c"
    ),
    "sortedMap" to mapOf(
        "c" to 3,
        "b" to 2,
        "a" to 1
    ),
    "listOfMap" to listOf(
        mapOf("a" to 1, "b" to 2),
        mapOf("a" to 3, "b" to 4)
    ),

    "nested" to listOf(listOf(listOf(mapOf("a" to 1)))),

    "pair" to mapOf("first" to 1, "second" to 2),

    "clazz" to mapOf(
        "empty" to "null",
        "literalEmpty" to "null",
        "present" to 1,

        "boolean" to false,

        "int" to 1,
        "short" to 2.toShort(),
        "byte" to 3.toByte(),
        "bigInteger" to BigInteger.valueOf(4),
        "long" to 4L,

        "double" to 1.5,
        "float" to -1.5f,
        "bigDecimal" to BigDecimal.valueOf(1.5),

        "char" to 'a',

        "string" to "string",
        "offsetTime" to OffsetTime.parse("10:15:30+01:00"),
        "offsetDateTime" to OffsetDateTime.parse("2007-12-03T10:15:30+01:00"),
        "zonedDateTime" to ZonedDateTime.parse("2007-12-03T10:15:30+01:00[Europe/Paris]"),
        "localDate" to LocalDate.parse("2007-12-03"),
        "localTime" to LocalTime.parse("10:15:30"),
        "localDateTime" to LocalDateTime.parse("2007-12-03T10:15:30"),
        "date" to Date.from(Instant.parse("2007-12-03T10:15:30Z")),
        "year" to Year.parse("2007"),
        "yearMonth" to YearMonth.parse("2007-12"),
        "instant" to Instant.parse("2007-12-03T10:15:30.00Z"),
        "duration" to "P2DT3H4M".toDuration(),
        "simpleDuration" to "200millis".toDuration(),
        "size" to "10k".toSizeInBytes(),

        "enum" to "LABEL2",

        "booleanArray" to listOf(true, false),

        "nested" to listOf(listOf(listOf(mapOf("a" to 1))))
    )
).mapKeys { (key, _) -> "level1.level2.$key" }
