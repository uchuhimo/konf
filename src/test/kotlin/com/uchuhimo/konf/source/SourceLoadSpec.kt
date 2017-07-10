package com.uchuhimo.konf.source

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.SizeInBytes
import com.uchuhimo.konf.assertTrue
import com.uchuhimo.konf.toSizeInBytes
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek
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
import java.util.Arrays
import java.util.Date
import java.util.SortedSet

object SourceLoadSpec : SubjectSpek<Config>({

    subject {
        Config {
            addSpec(ConfigForLoad)
        }.loadFrom.map.kv(loadContent)
    }
    given("a source") {
        on("load the source into config") {
            it("should contain every value specified in the source") {
                assertThat(subject[ConfigForLoad.booleanItem], equalTo(false))

                assertThat(subject[ConfigForLoad.intItem], equalTo(1))
                assertThat(subject[ConfigForLoad.shortItem], equalTo(2.toShort()))
                assertThat(subject[ConfigForLoad.byteItem], equalTo(3.toByte()))
                assertThat(subject[ConfigForLoad.bigIntegerItem], equalTo(BigInteger.valueOf(4)))
                assertThat(subject[ConfigForLoad.longItem], equalTo(4L))

                assertThat(subject[ConfigForLoad.doubleItem], equalTo(1.5))
                assertThat(subject[ConfigForLoad.floatItem], equalTo(-1.5f))
                assertThat(subject[ConfigForLoad.bigDecimalItem], equalTo(BigDecimal.valueOf(1.5)))

                assertThat(subject[ConfigForLoad.charItem], equalTo('a'))

                assertThat(subject[ConfigForLoad.stringItem], equalTo("string"))
                assertThat(subject[ConfigForLoad.offsetTimeItem],
                        equalTo(OffsetTime.parse("10:15:30+01:00")))
                assertThat(subject[ConfigForLoad.offsetDateTimeItem],
                        equalTo(OffsetDateTime.parse("2007-12-03T10:15:30+01:00")))
                assertThat(subject[ConfigForLoad.zonedDateTimeItem],
                        equalTo(ZonedDateTime.parse("2007-12-03T10:15:30+01:00[Europe/Paris]")))
                assertThat(subject[ConfigForLoad.localDateItem],
                        equalTo(LocalDate.parse("2007-12-03")))
                assertThat(subject[ConfigForLoad.localTimeItem],
                        equalTo(LocalTime.parse("10:15:30")))
                assertThat(subject[ConfigForLoad.localDateTimeItem],
                        equalTo(LocalDateTime.parse("2007-12-03T10:15:30")))
                assertThat(subject[ConfigForLoad.dateItem],
                        equalTo(Date.from(Instant.parse("2007-12-03T10:15:30Z"))))
                assertThat(subject[ConfigForLoad.yearItem],
                        equalTo(Year.parse("2007")))
                assertThat(subject[ConfigForLoad.yearMonthItem],
                        equalTo(YearMonth.parse("2007-12")))
                assertThat(subject[ConfigForLoad.instantItem],
                        equalTo(Instant.parse("2007-12-03T10:15:30.00Z")))
                assertThat(subject[ConfigForLoad.durationItem],
                        equalTo(Duration.parse("P2DT3H4M")))
                assertThat(subject[ConfigForLoad.simpleDurationItem],
                        equalTo(Duration.ofMillis(200)))
                assertThat(subject[ConfigForLoad.sizeItem].bytes, equalTo(10240L))

                assertThat(subject[ConfigForLoad.enumItem], equalTo(EnumForLoad.LABEL2))

                // array items
                assertTrue(Arrays.equals(
                        subject[ConfigForLoad.booleanArrayItem],
                        booleanArrayOf(true, false)))
                assertTrue(Arrays.equals(
                        subject[ConfigForLoad.intArrayItem],
                        intArrayOf(1, 2, 3)))
                assertTrue(Arrays.equals(
                        subject[ConfigForLoad.longArrayItem],
                        longArrayOf(4, 5, 6)))
                assertTrue(Arrays.equals(
                        subject[ConfigForLoad.doubleArrayItem],
                        doubleArrayOf(-1.0, 0.0, 1.0)))
                assertTrue(Arrays.equals(
                        subject[ConfigForLoad.charArrayItem],
                        charArrayOf('a', 'b', 'c')))

                // object array items
                assertTrue(Arrays.equals(
                        subject[ConfigForLoad.booleanObjectArrayItem],
                        arrayOf(true, false)))
                assertTrue(Arrays.equals(
                        subject[ConfigForLoad.intObjectArrayItem],
                        arrayOf(1, 2, 3)))
                assertTrue(Arrays.equals(
                        subject[ConfigForLoad.stringArrayItem],
                        arrayOf("one", "two", "three")))
                assertTrue(Arrays.equals(
                        subject[ConfigForLoad.enumArrayItem],
                        arrayOf(EnumForLoad.LABEL1, EnumForLoad.LABEL2, EnumForLoad.LABEL3)))

                assertThat(subject[ConfigForLoad.listItem], equalTo(listOf(1, 2, 3)))

                assertTrue(Arrays.equals(
                        subject[ConfigForLoad.mutableListItem].toTypedArray(),
                        arrayOf(1, 2, 3)))

                assertThat(subject[ConfigForLoad.listOfListItem],
                        equalTo(listOf(listOf(1, 2), listOf(3, 4))))

                assertThat(subject[ConfigForLoad.setItem], equalTo(setOf(1, 2)))

                assertThat(subject[ConfigForLoad.sortedSetItem],
                        equalTo<SortedSet<Int>>(sortedSetOf(1, 2, 3)))

                assertThat(subject[ConfigForLoad.mapItem],
                        equalTo(mapOf("a" to 1, "b" to 2, "c" to 3)))
                assertThat(subject[ConfigForLoad.sortedMapItem],
                        equalTo(sortedMapOf("a" to 1, "b" to 2, "c" to 3)))
                assertThat(subject[ConfigForLoad.sortedMapItem].firstKey(), equalTo("a"))
                assertThat(subject[ConfigForLoad.sortedMapItem].lastKey(), equalTo("c"))
                assertThat(subject[ConfigForLoad.listOfMapItem],
                        equalTo(listOf(mapOf("a" to 1, "b" to 2), mapOf("a" to 3, "b" to 4))))

                assertTrue(Arrays.equals(subject[ConfigForLoad.nestedItem],
                        arrayOf(listOf(setOf(mapOf("a" to 1))))))

                val x = subject
                assertThat(subject[ConfigForLoad.pairItem], equalTo(1 to 2))

                val classForLoad = ClassForLoad(
                        boolean = false,
                        int = 1,
                        short = 2.toShort(),
                        byte = 3.toByte(),
                        bigInteger = BigInteger.valueOf(4),
                        long = 4L,
                        double = 1.5,
                        float = -1.5f,
                        bigDecimal = BigDecimal.valueOf(1.5),
                        char = 'a',
                        string = "string",
                        offsetTime = OffsetTime.parse("10:15:30+01:00"),
                        offsetDateTime = OffsetDateTime.parse("2007-12-03T10:15:30+01:00"),
                        zonedDateTime = ZonedDateTime.parse("2007-12-03T10:15:30+01:00[Europe/Paris]"),
                        localDate = LocalDate.parse("2007-12-03"),
                        localTime = LocalTime.parse("10:15:30"),
                        localDateTime = LocalDateTime.parse("2007-12-03T10:15:30"),
                        date = Date.from(Instant.parse("2007-12-03T10:15:30Z")),
                        year = Year.parse("2007"),
                        yearMonth = YearMonth.parse("2007-12"),
                        instant = Instant.parse("2007-12-03T10:15:30.00Z"),
                        duration = "P2DT3H4M".toDuration(),
                        simpleDuration = Duration.ofMillis(200),
                        size = SizeInBytes.parse("10k"),
                        enum = EnumForLoad.LABEL2,
                        booleanArray = booleanArrayOf(true, false),
                        nested = arrayOf(listOf(setOf(mapOf("a" to 1)))))
                assertThat(subject[ConfigForLoad.classItem].boolean, equalTo(classForLoad.boolean))
                assertThat(subject[ConfigForLoad.classItem].int, equalTo(classForLoad.int))
                assertThat(subject[ConfigForLoad.classItem].short, equalTo(classForLoad.short))
                assertThat(subject[ConfigForLoad.classItem].byte, equalTo(classForLoad.byte))
                assertThat(subject[ConfigForLoad.classItem].bigInteger, equalTo(classForLoad.bigInteger))
                assertThat(subject[ConfigForLoad.classItem].long, equalTo(classForLoad.long))
                assertThat(subject[ConfigForLoad.classItem].double, equalTo(classForLoad.double))
                assertThat(subject[ConfigForLoad.classItem].float, equalTo(classForLoad.float))
                assertThat(subject[ConfigForLoad.classItem].bigDecimal, equalTo(classForLoad.bigDecimal))
                assertThat(subject[ConfigForLoad.classItem].char, equalTo(classForLoad.char))
                assertThat(subject[ConfigForLoad.classItem].string, equalTo(classForLoad.string))
                assertThat(subject[ConfigForLoad.classItem].offsetTime, equalTo(classForLoad.offsetTime))
                assertThat(subject[ConfigForLoad.classItem].offsetDateTime, equalTo(classForLoad.offsetDateTime))
                assertThat(subject[ConfigForLoad.classItem].zonedDateTime, equalTo(classForLoad.zonedDateTime))
                assertThat(subject[ConfigForLoad.classItem].localDate, equalTo(classForLoad.localDate))
                assertThat(subject[ConfigForLoad.classItem].localTime, equalTo(classForLoad.localTime))
                assertThat(subject[ConfigForLoad.classItem].localDateTime, equalTo(classForLoad.localDateTime))
                assertThat(subject[ConfigForLoad.classItem].date, equalTo(classForLoad.date))
                assertThat(subject[ConfigForLoad.classItem].year, equalTo(classForLoad.year))
                assertThat(subject[ConfigForLoad.classItem].yearMonth, equalTo(classForLoad.yearMonth))
                assertThat(subject[ConfigForLoad.classItem].instant, equalTo(classForLoad.instant))
                assertThat(subject[ConfigForLoad.classItem].duration, equalTo(classForLoad.duration))
                assertThat(subject[ConfigForLoad.classItem].simpleDuration, equalTo(classForLoad.simpleDuration))
                assertThat(subject[ConfigForLoad.classItem].size, equalTo(classForLoad.size))
                assertThat(subject[ConfigForLoad.classItem].enum, equalTo(classForLoad.enum))
                assertTrue(Arrays.equals(subject[ConfigForLoad.classItem].booleanArray, classForLoad.booleanArray))
                assertTrue(Arrays.equals(subject[ConfigForLoad.classItem].nested, classForLoad.nested))
            }
        }
    }
})

private val loadContent = mapOf<String, Any>(
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
        "array.int" to listOf(1, 2, 3),
        "array.long" to listOf(4L, 5L, 6L),
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
).mapKeys { (key, _) -> "level1.level2.$key" }
