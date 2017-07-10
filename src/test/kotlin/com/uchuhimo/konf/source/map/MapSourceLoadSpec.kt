package com.uchuhimo.konf.source.map

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.ConfigForLoad
import com.uchuhimo.konf.source.SourceLoadSpec
import com.uchuhimo.konf.source.toDuration
import com.uchuhimo.konf.toSizeInBytes
import org.jetbrains.spek.subject.SubjectSpek
import org.jetbrains.spek.subject.itBehavesLike
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

object MapSourceLoadSpec : SubjectSpek<Config>({

    subject {
        Config {
            addSpec(ConfigForLoad)
        }.loadFrom.map.hierarchical(loadContent)
    }

    itBehavesLike(SourceLoadSpec)
})

private val loadContent = mapOf("level1" to mapOf("level2" to
        mapOf<String, Any>(
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

                "array" to mapOf(
                        "boolean" to listOf(true, false),
                        "int" to listOf(1, 2, 3),
                        "long" to listOf(4L, 5L, 6L),
                        "double" to listOf(-1.0, 0.0, 1.0),
                        "char" to listOf('a', 'b', 'c'),

                        "object" to mapOf(
                                "boolean" to listOf(true, false),
                                "int" to listOf(1, 2, 3),
                                "string" to listOf("one", "two", "three"),
                                "enum" to listOf("LABEL1", "LABEL2", "LABEL3")
                        )
                ),

                "list" to listOf(1, 2, 3),
                "mutableList" to listOf(1, 2, 3),
                "listOfList" to listOf(listOf(1, 2), listOf(3, 4)),
                "set" to listOf(1, 2, 1),
                "sortedSet" to listOf(2, 1, 1, 3),

                "map" to mapOf(
                        "a" to 1,
                        "b" to 2,
                        "c" to 3),
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

                "class" to mapOf(
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
        )
))
