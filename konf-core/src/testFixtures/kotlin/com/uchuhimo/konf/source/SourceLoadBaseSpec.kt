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

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.SizeInBytes
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

object SourceLoadBaseSpec : SubjectSpek<Config>({
    given("a source") {
        on("load the source into config") {
            it("should contain every value specified in the source") {
                assertNull(subject[ConfigForLoad.empty])
                assertNull(subject[ConfigForLoad.literalEmpty])
                assertThat(subject[ConfigForLoad.present], equalTo(1))
                assertThat(subject[ConfigForLoad.boolean], equalTo(false))

                assertThat(subject[ConfigForLoad.int], equalTo(1))
                assertThat(subject[ConfigForLoad.short], equalTo(2.toShort()))
                assertThat(subject[ConfigForLoad.byte], equalTo(3.toByte()))
                assertThat(subject[ConfigForLoad.bigInteger], equalTo(BigInteger.valueOf(4)))
                assertThat(subject[ConfigForLoad.long], equalTo(4L))

                assertThat(subject[ConfigForLoad.double], equalTo(1.5))
                assertThat(subject[ConfigForLoad.float], equalTo(-1.5f))
                assertThat(subject[ConfigForLoad.bigDecimal], equalTo(BigDecimal.valueOf(1.5)))

                assertThat(subject[ConfigForLoad.char], equalTo('a'))

                assertThat(subject[ConfigForLoad.string], equalTo("string"))
                assertThat(
                    subject[ConfigForLoad.offsetTime],
                    equalTo(OffsetTime.parse("10:15:30+01:00"))
                )
                assertThat(
                    subject[ConfigForLoad.offsetDateTime],
                    equalTo(OffsetDateTime.parse("2007-12-03T10:15:30+01:00"))
                )
                assertThat(
                    subject[ConfigForLoad.zonedDateTime],
                    equalTo(ZonedDateTime.parse("2007-12-03T10:15:30+01:00[Europe/Paris]"))
                )
                assertThat(
                    subject[ConfigForLoad.localDate],
                    equalTo(LocalDate.parse("2007-12-03"))
                )
                assertThat(
                    subject[ConfigForLoad.localTime],
                    equalTo(LocalTime.parse("10:15:30"))
                )
                assertThat(
                    subject[ConfigForLoad.localDateTime],
                    equalTo(LocalDateTime.parse("2007-12-03T10:15:30"))
                )
                assertThat(
                    subject[ConfigForLoad.date],
                    equalTo(Date.from(Instant.parse("2007-12-03T10:15:30Z")))
                )
                assertThat(
                    subject[ConfigForLoad.year],
                    equalTo(Year.parse("2007"))
                )
                assertThat(
                    subject[ConfigForLoad.yearMonth],
                    equalTo(YearMonth.parse("2007-12"))
                )
                assertThat(
                    subject[ConfigForLoad.instant],
                    equalTo(Instant.parse("2007-12-03T10:15:30.00Z"))
                )
                assertThat(
                    subject[ConfigForLoad.duration],
                    equalTo(Duration.parse("P2DT3H4M"))
                )
                assertThat(
                    subject[ConfigForLoad.simpleDuration],
                    equalTo(Duration.ofMillis(200))
                )
                assertThat(subject[ConfigForLoad.size].bytes, equalTo(10240L))

                assertThat(subject[ConfigForLoad.enum], equalTo(EnumForLoad.LABEL2))

                // array items
                assertTrue(
                    Arrays.equals(
                        subject[ConfigForLoad.booleanArray],
                        booleanArrayOf(true, false)
                    )
                )
                assertTrue(
                    Arrays.equals(
                        subject[ConfigForLoad.byteArray],
                        byteArrayOf(1, 2, 3)
                    )
                )
                assertTrue(
                    Arrays.equals(
                        subject[ConfigForLoad.shortArray],
                        shortArrayOf(1, 2, 3)
                    )
                )
                assertTrue(
                    Arrays.equals(
                        subject[ConfigForLoad.intArray],
                        intArrayOf(1, 2, 3)
                    )
                )
                assertTrue(
                    Arrays.equals(
                        subject[ConfigForLoad.longArray],
                        longArrayOf(4, 5, 6)
                    )
                )
                assertTrue(
                    Arrays.equals(
                        subject[ConfigForLoad.floatArray],
                        floatArrayOf(-1.0F, 0.0F, 1.0F)
                    )
                )
                assertTrue(
                    Arrays.equals(
                        subject[ConfigForLoad.doubleArray],
                        doubleArrayOf(-1.0, 0.0, 1.0)
                    )
                )
                assertTrue(
                    Arrays.equals(
                        subject[ConfigForLoad.charArray],
                        charArrayOf('a', 'b', 'c')
                    )
                )

                // object array items
                assertTrue(
                    Arrays.equals(
                        subject[ConfigForLoad.booleanObjectArray],
                        arrayOf(true, false)
                    )
                )
                assertTrue(
                    Arrays.equals(
                        subject[ConfigForLoad.intObjectArray],
                        arrayOf(1, 2, 3)
                    )
                )
                assertTrue(
                    Arrays.equals(
                        subject[ConfigForLoad.stringArray],
                        arrayOf("one", "two", "three")
                    )
                )
                assertTrue(
                    Arrays.equals(
                        subject[ConfigForLoad.enumArray],
                        arrayOf(EnumForLoad.LABEL1, EnumForLoad.LABEL2, EnumForLoad.LABEL3)
                    )
                )

                assertThat(subject[ConfigForLoad.list], equalTo(listOf(1, 2, 3)))

                assertTrue(
                    Arrays.equals(
                        subject[ConfigForLoad.mutableList].toTypedArray(),
                        arrayOf(1, 2, 3)
                    )
                )

                assertThat(
                    subject[ConfigForLoad.listOfList],
                    equalTo(listOf(listOf(1, 2), listOf(3, 4)))
                )

                assertThat(subject[ConfigForLoad.set], equalTo(setOf(1, 2)))

                assertThat(
                    subject[ConfigForLoad.sortedSet],
                    equalTo<SortedSet<Int>>(sortedSetOf(1, 2, 3))
                )

                assertThat(
                    subject[ConfigForLoad.map],
                    equalTo(mapOf("a" to 1, "b" to 2, "c" to 3))
                )
                assertThat(
                    subject[ConfigForLoad.intMap],
                    equalTo(mapOf(1 to "a", 2 to "b", 3 to "c"))
                )
                assertThat(
                    subject[ConfigForLoad.sortedMap],
                    equalTo(sortedMapOf("a" to 1, "b" to 2, "c" to 3))
                )
                assertThat(subject[ConfigForLoad.sortedMap].firstKey(), equalTo("a"))
                assertThat(subject[ConfigForLoad.sortedMap].lastKey(), equalTo("c"))
                assertThat(
                    subject[ConfigForLoad.listOfMap],
                    equalTo(listOf(mapOf("a" to 1, "b" to 2), mapOf("a" to 3, "b" to 4)))
                )

                assertTrue(
                    Arrays.equals(
                        subject[ConfigForLoad.nested],
                        arrayOf(listOf(setOf(mapOf("a" to 1))))
                    )
                )

                assertThat(subject[ConfigForLoad.pair], equalTo(1 to 2))

                val classForLoad = ClassForLoad(
                    empty = null,
                    literalEmpty = null,
                    present = 1,
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
                    nested = arrayOf(listOf(setOf(mapOf("a" to 1))))
                )
                assertThat(subject[ConfigForLoad.clazz].empty, equalTo(classForLoad.empty))
                assertThat(subject[ConfigForLoad.clazz].literalEmpty, equalTo(classForLoad.literalEmpty))
                assertThat(subject[ConfigForLoad.clazz].present, equalTo(classForLoad.present))
                assertThat(subject[ConfigForLoad.clazz].boolean, equalTo(classForLoad.boolean))
                assertThat(subject[ConfigForLoad.clazz].int, equalTo(classForLoad.int))
                assertThat(subject[ConfigForLoad.clazz].short, equalTo(classForLoad.short))
                assertThat(subject[ConfigForLoad.clazz].byte, equalTo(classForLoad.byte))
                assertThat(subject[ConfigForLoad.clazz].bigInteger, equalTo(classForLoad.bigInteger))
                assertThat(subject[ConfigForLoad.clazz].long, equalTo(classForLoad.long))
                assertThat(subject[ConfigForLoad.clazz].double, equalTo(classForLoad.double))
                assertThat(subject[ConfigForLoad.clazz].float, equalTo(classForLoad.float))
                assertThat(subject[ConfigForLoad.clazz].bigDecimal, equalTo(classForLoad.bigDecimal))
                assertThat(subject[ConfigForLoad.clazz].char, equalTo(classForLoad.char))
                assertThat(subject[ConfigForLoad.clazz].string, equalTo(classForLoad.string))
                assertThat(subject[ConfigForLoad.clazz].offsetTime, equalTo(classForLoad.offsetTime))
                assertThat(subject[ConfigForLoad.clazz].offsetDateTime, equalTo(classForLoad.offsetDateTime))
                assertThat(subject[ConfigForLoad.clazz].zonedDateTime, equalTo(classForLoad.zonedDateTime))
                assertThat(subject[ConfigForLoad.clazz].localDate, equalTo(classForLoad.localDate))
                assertThat(subject[ConfigForLoad.clazz].localTime, equalTo(classForLoad.localTime))
                assertThat(subject[ConfigForLoad.clazz].localDateTime, equalTo(classForLoad.localDateTime))
                assertThat(subject[ConfigForLoad.clazz].date, equalTo(classForLoad.date))
                assertThat(subject[ConfigForLoad.clazz].year, equalTo(classForLoad.year))
                assertThat(subject[ConfigForLoad.clazz].yearMonth, equalTo(classForLoad.yearMonth))
                assertThat(subject[ConfigForLoad.clazz].instant, equalTo(classForLoad.instant))
                assertThat(subject[ConfigForLoad.clazz].duration, equalTo(classForLoad.duration))
                assertThat(subject[ConfigForLoad.clazz].simpleDuration, equalTo(classForLoad.simpleDuration))
                assertThat(subject[ConfigForLoad.clazz].size, equalTo(classForLoad.size))
                assertThat(subject[ConfigForLoad.clazz].enum, equalTo(classForLoad.enum))
                assertTrue(Arrays.equals(subject[ConfigForLoad.clazz].booleanArray, classForLoad.booleanArray))
                assertTrue(Arrays.equals(subject[ConfigForLoad.clazz].nested, classForLoad.nested))
            }
        }
    }
})
