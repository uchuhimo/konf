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

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.throws
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import com.uchuhimo.konf.Feature
import com.uchuhimo.konf.NetworkBuffer
import com.uchuhimo.konf.Prefix
import com.uchuhimo.konf.UnsetValueException
import com.uchuhimo.konf.name
import com.uchuhimo.konf.required
import com.uchuhimo.konf.source.base.ValueSource
import com.uchuhimo.konf.source.base.asKVSource
import com.uchuhimo.konf.source.base.asSource
import com.uchuhimo.konf.toPath
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.junit.jupiter.api.assertThrows
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
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.assertFalse
import kotlin.test.assertTrue

object SourceSpec : Spek({
    given("a source") {
        group("get operation") {
            val value: Source = ValueSource(Unit)
            val validPath = "a.b".toPath()
            val invalidPath = "a.c".toPath()
            val validKey = "a"
            val invalidKey = "b"
            val sourceForPath by memoized { mapOf(validPath.name to value).asKVSource() }
            val sourceForKey by memoized { mapOf(validKey to value).asSource() }
            on("find a valid path") {
                it("should contain the value") {
                    assertTrue(validPath in sourceForPath)
                }
            }
            on("find an invalid path") {
                it("should not contain the value") {
                    assertTrue(invalidPath !in sourceForPath)
                }
            }

            on("get by a valid path using `getOrNull`") {
                it("should return the corresponding value") {
                    assertThat(sourceForPath.getOrNull(validPath), equalTo(value))
                }
            }
            on("get by an invalid path using `getOrNull`") {
                it("should return null") {
                    assertThat(sourceForPath.getOrNull(invalidPath), absent())
                }
            }

            on("get by a valid path using `get`") {
                it("should return the corresponding value") {
                    assertThat(sourceForPath[validPath], equalTo(value))
                }
            }
            on("get by an invalid path using `get`") {
                it("should throw NoSuchPathException") {
                    assertThat({ sourceForPath[invalidPath] },
                        throws(has(NoSuchPathException::path, equalTo(invalidPath))))
                }
            }

            on("find a valid key") {
                it("should contain the value") {
                    assertTrue(validKey in sourceForKey)
                }
            }
            on("find an invalid key") {
                it("should not contain the value") {
                    assertTrue(invalidKey !in sourceForKey)
                }
            }

            on("get by a valid key using `getOrNull`") {
                it("should return the corresponding value") {
                    assertThat(sourceForKey.getOrNull(validKey), equalTo(value))
                }
            }
            on("get by an invalid key using `getOrNull`") {
                it("should return null") {
                    assertThat(sourceForKey.getOrNull(invalidKey), absent())
                }
            }

            on("get by a valid key using `get`") {
                it("should return the corresponding value") {
                    assertThat(sourceForKey[validKey], equalTo(value))
                }
            }
            on("get by an invalid key using `get`") {
                it("should throw NoSuchPathException") {
                    assertThat({ sourceForKey[invalidKey] },
                        throws(has(NoSuchPathException::path, equalTo(invalidKey.toPath()))))
                }
            }
        }
        group("cast operation") {
            on("cast int to long") {
                val source = 1.asSource()
                it("should succeed") {
                    assertThat(source.toLong(), equalTo(1L))
                }
            }
            on("cast int in range of short to short") {
                val source = 1.asSource()
                it("should succeed") {
                    assertThat(source.toShort(), equalTo(1.toShort()))
                }
            }
            on("cast int out of range of short to short") {
                it("should throw ParseException") {
                    assertThat({ Int.MAX_VALUE.asSource().toShort() }, throws<ParseException>())
                    assertThat({ Int.MIN_VALUE.asSource().toShort() }, throws<ParseException>())
                }
            }

            on("cast int in range of byte to byte") {
                val source = 1.asSource()
                it("should succeed") {
                    assertThat(source.toByte(), equalTo(1.toByte()))
                }
            }
            on("cast int out of range of byte to byte") {
                it("should throw ParseException") {
                    assertThat({ Int.MAX_VALUE.asSource().toByte() }, throws<ParseException>())
                    assertThat({ Int.MIN_VALUE.asSource().toByte() }, throws<ParseException>())
                }
            }

            on("cast double to float") {
                val source = 1.5.asSource()
                it("should succeed") {
                    assertThat(source.toFloat(), equalTo(1.5f))
                }
            }

            on("cast string containing single char to char") {
                val source = "a".asSource()
                it("should succeed") {
                    assertThat(source.toChar(), equalTo('a'))
                }
            }
            on("cast string containing multiple chars to char") {
                val source = "ab".asSource()
                it("should throw WrongTypeException") {
                    assertThat({ source.toChar() }, throws<WrongTypeException>())
                }
            }

            on("cast long to BigInteger") {
                val source = 1L.asSource()
                it("should succeed") {
                    assertThat(source.toBigInteger(), equalTo(BigInteger.valueOf(1)))
                }
            }

            on("cast double to BigDecimal") {
                val source = 1.5.asSource()
                it("should succeed") {
                    assertThat(source.toBigDecimal(), equalTo(BigDecimal.valueOf(1.5)))
                }
            }

            on("cast string to OffsetTime") {
                val text = "10:15:30+01:00"
                val source = text.asSource()
                it("should succeed") {
                    assertThat(source.toOffsetTime(), equalTo(OffsetTime.parse(text)))
                }
            }
            on("cast string with invalid format to OffsetTime") {
                val text = "10:15:30"
                val source = text.asSource()
                it("should throw ParseException") {
                    assertThat({ source.toOffsetTime() }, throws<ParseException>())
                }
            }

            on("cast string to OffsetDateTime") {
                val text = "2007-12-03T10:15:30+01:00"
                val source = text.asSource()
                it("should succeed") {
                    assertThat(source.toOffsetDateTime(), equalTo(OffsetDateTime.parse(text)))
                }
            }

            on("cast string to ZonedDateTime") {
                val text = "2007-12-03T10:15:30+01:00[Europe/Paris]"
                val source = text.asSource()
                it("should succeed") {
                    assertThat(source.toZonedDateTime(), equalTo(ZonedDateTime.parse(text)))
                }
            }

            on("cast string to LocalDate") {
                val text = "2007-12-03"
                val source = text.asSource()
                it("should succeed") {
                    assertThat(source.toLocalDate(), equalTo(LocalDate.parse(text)))
                }
            }

            on("cast string to LocalTime") {
                val text = "10:15:30"
                val source = text.asSource()
                it("should succeed") {
                    assertThat(source.toLocalTime(), equalTo(LocalTime.parse(text)))
                }
            }

            on("cast string to LocalDateTime") {
                val text = "2007-12-03T10:15:30"
                val source = text.asSource()
                it("should succeed") {
                    assertThat(source.toLocalDateTime(), equalTo(LocalDateTime.parse(text)))
                }
            }

            on("cast string to Year") {
                val text = "2007"
                val source = text.asSource()
                it("should succeed") {
                    assertThat(source.toYear(), equalTo(Year.parse(text)))
                }
            }

            on("cast string to YearMonth") {
                val text = "2007-12"
                val source = text.asSource()
                it("should succeed") {
                    assertThat(source.toYearMonth(), equalTo(YearMonth.parse(text)))
                }
            }

            on("cast string to Instant") {
                val text = "2007-12-03T10:15:30.00Z"
                val source = text.asSource()
                it("should succeed") {
                    assertThat(source.toInstant(), equalTo(Instant.parse(text)))
                }
            }

            on("cast string to Date") {
                val text = "2007-12-03T10:15:30.00Z"
                val source = text.asSource()
                it("should succeed") {
                    assertThat(source.toDate(), equalTo(Date.from(Instant.parse(text))))
                }
            }

            on("cast LocalDateTime string to Date") {
                val text = "2007-12-03T10:15:30"
                val source = text.asSource()
                it("should succeed") {
                    assertThat(source.toDate(),
                        equalTo(Date.from(LocalDateTime.parse(text).toInstant(ZoneOffset.UTC))))
                }
            }

            on("cast LocalDate string to Date") {
                val text = "2007-12-03"
                val source = text.asSource()
                it("should succeed") {
                    assertThat(source.toDate(),
                        equalTo(Date.from(LocalDate.parse(text).atStartOfDay().toInstant(ZoneOffset.UTC))))
                }
            }

            on("cast string to Duration") {
                val text = "P2DT3H4M"
                val source = text.asSource()
                it("should succeed") {
                    assertThat(source.toDuration(), equalTo(Duration.parse(text)))
                }
            }

            on("cast string with simple unit to Duration") {
                val text = "200ms"
                val source = text.asSource()
                it("should succeed") {
                    assertThat(source.toDuration(), equalTo(Duration.ofMillis(200)))
                }
            }
            on("cast string with invalid format to Duration") {
                val text = "2 year"
                val source = text.asSource()
                it("should throw ParseException") {
                    assertThat({ source.toDuration() }, throws<ParseException>())
                }
            }

            on("cast string to SizeInBytes") {
                val text = "10k"
                val source = text.asSource()
                it("should succeed") {
                    assertThat(source.toSizeInBytes().bytes, equalTo(10240L))
                }
            }
            on("cast string with invalid format to SizeInBytes") {
                val text = "10u"
                val source = text.asSource()
                it("should throw ParseException") {
                    assertThat({ source.toSizeInBytes() }, throws<ParseException>())
                }
            }
        }
        group("load operation") {
            on("load from valid source") {
                it("should load successfully") {
                    val config = load<Int>(1)
                    assertThat(config("item"), equalTo(1))
                }
            }
            on("load concrete map type") {
                it("should load successfully") {
                    val config = load<ConcurrentHashMap<String, Int>>(mapOf("1" to 1))
                    assertThat(config<ConcurrentHashMap<String, Int>>("item"), equalTo(mapOf("1" to 1)))
                }
            }
            on("load invalid enum value") {
                it("should throw LoadException caused by ParseException") {
                    assertCausedBy<ParseException> {
                        load<NetworkBuffer.Type>("NO_HEAP")
                    }
                }
            }
            on("load unsupported simple type value") {
                it("should throw LoadException caused by ObjectMappingException") {
                    assertCausedBy<ObjectMappingException> {
                        load<Person>(mapOf("invalid" to "anon"))
                    }
                }
            }
            on("load map with unsupported key type") {
                it("should throw LoadException caused by UnsupportedMapKeyException") {
                    assertCausedBy<UnsupportedMapKeyException> {
                        load<Map<Int, String>>(mapOf(1 to "1"))
                    }
                }
            }
            on("load invalid enum value") {
                it("should throw LoadException caused by ParseException") {
                    assertCausedBy<ParseException> {
                        load<NetworkBuffer.Type>("NO_HEAP")
                    }
                }
            }
            on("load invalid POJO value") {
                it("should throw LoadException caused by ParseException") {
                    assertCausedBy<ParseException> {
                        load<Person>(mapOf("name" to DumbSource()))
                    }
                }
            }
        }
        group("feature operation") {
            on("enable feature") {
                val source = DumbSource().enabled(Feature.FAIL_ON_UNKNOWN_PATH)
                it("should let the feature be enabled") {
                    assertTrue { source.isEnabled(Feature.FAIL_ON_UNKNOWN_PATH) }
                }
            }
            on("disable feature") {
                val source = DumbSource().disabled(Feature.FAIL_ON_UNKNOWN_PATH)
                it("should let the feature be disabled") {
                    assertFalse { source.isEnabled(Feature.FAIL_ON_UNKNOWN_PATH) }
                }
            }
            on("by default") {
                val source = DumbSource()
                it("should use the feature's default setting") {
                    assertThat(
                        source.isEnabled(Feature.FAIL_ON_UNKNOWN_PATH),
                        equalTo(Feature.FAIL_ON_UNKNOWN_PATH.enabledByDefault))
                }
            }
            on("enable LOAD_KEYS_CASE_INSENSITIVELY feature") {
                val source = mapOf("somekey" to "value").asSource().enabled(Feature.LOAD_KEYS_CASE_INSENSITIVELY)
                val config = Config().withSource(source)
                it("should load keys case-insensitively") {
                    val someKey by config.required<String>()
                    assertThat(someKey, equalTo("value"))
                }
            }
            on("disable LOAD_KEYS_CASE_INSENSITIVELY feature") {
                val source = mapOf("somekey" to "value").asSource().disabled(Feature.LOAD_KEYS_CASE_INSENSITIVELY)
                val config = Config().withSource(source)
                it("should load keys case-sensitively") {
                    val someKey by config.required<String>()
                    assertThrows<UnsetValueException> { someKey }
                    val somekey by config.required<String>()
                    assertThat(somekey, equalTo("value"))
                }
            }
        }
        group("default implementations") {
            val source = DumbSource()
            it("returns `false` for all `is` operations") {
                assertFalse(source.isBigDecimal())
                assertFalse(source.isBigInteger())
                assertFalse(source.isBoolean())
                assertFalse(source.isByte())
                assertFalse(source.isChar())
                assertFalse(source.isDate())
                assertFalse(source.isDouble())
                assertFalse(source.isDuration())
                assertFalse(source.isFloat())
                assertFalse(source.isInstant())
                assertFalse(source.isInt())
                assertFalse(source.isList())
                assertFalse(source.isLocalDate())
                assertFalse(source.isLocalDateTime())
                assertFalse(source.isLocalTime())
                assertFalse(source.isLong())
                assertFalse(source.isMap())
                assertFalse(source.isOffsetDateTime())
                assertFalse(source.isOffsetTime())
                assertFalse(source.isShort())
                assertFalse(source.isSizeInBytes())
                assertFalse(source.isText())
                assertFalse(source.isYear())
                assertFalse(source.isYearMonth())
                assertFalse(source.isZonedDateTime())
            }
            it("throws UnsupportedOperationException for all cast operations") {
                assertThat({ source.toList() }, throws<UnsupportedOperationException>())
                assertThat({ source.toMap() }, throws<UnsupportedOperationException>())
                assertThat({ source.toText() }, throws<UnsupportedOperationException>())
                assertThat({ source.toBoolean() }, throws<UnsupportedOperationException>())
                assertThat({ source.toLong() }, throws<UnsupportedOperationException>())
                assertThat({ source.toDouble() }, throws<UnsupportedOperationException>())
                assertThat({ source.toInt() }, throws<UnsupportedOperationException>())
            }
        }
        group("source with prefix") {
            val source by memoized {
                Prefix("level1.level2") + mapOf("key" to "value").asSource()
            }
            on("find a valid path") {
                it("should contain the value") {
                    assertTrue("level1" in source)
                    assertTrue("level1.level2" in source)
                    assertTrue("level1.level2.key" in source)
                }
            }
            on("find an invalid path") {
                it("should not contain the value") {
                    assertTrue("level3" !in source)
                    assertTrue("level1.level3" !in source)
                    assertTrue("level1.level2.level3" !in source)
                    assertTrue("level1.level3.level2" !in source)
                }
            }

            on("get by a valid path using `getOrNull`") {
                it("should return the corresponding value") {
                    assertThat(
                        (source.getOrNull("level1")?.get("level2.key") as ValueSource).value as String,
                        equalTo("value"))
                    assertThat(
                        (source.getOrNull("level1.level2")?.get("key") as ValueSource).value as String,
                        equalTo("value"))
                    assertThat(
                        (source.getOrNull("level1.level2.key") as ValueSource).value as String,
                        equalTo("value"))
                }
            }
            on("get by an invalid path using `getOrNull`") {
                it("should return null") {
                    assertThat(source.getOrNull("level3"), absent())
                    assertThat(source.getOrNull("level1.level3"), absent())
                    assertThat(source.getOrNull("level1.level2.level3"), absent())
                    assertThat(source.getOrNull("level1.level3.level2"), absent())
                }
            }
        }
    }
})

private inline fun <reified T : Any> load(value: Any): Config =
    Config().apply {
        addSpec(object : ConfigSpec() {
            @Suppress("unused")
            val item by required<T>()
        })
    }.withSource(mapOf("item" to value).asSource())

private data class Person(val name: String)
