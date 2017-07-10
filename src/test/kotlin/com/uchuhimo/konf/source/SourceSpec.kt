package com.uchuhimo.konf.source

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.throws
import com.uchuhimo.konf.assertTrue
import com.uchuhimo.konf.name
import com.uchuhimo.konf.source.base.ValueSource
import com.uchuhimo.konf.source.base.asKVSource
import com.uchuhimo.konf.source.base.asSource
import com.uchuhimo.konf.toPath
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
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

object SourceSpec : Spek({
    given("a source") {
        group("get operation") {
            val value: Source = ValueSource(Unit)
            val validPath = listOf("a", "b")
            val invalidPath = listOf("a", "c")
            val validKey = "a"
            val invalidKey = "b"
            val sourceForPath by memoized { mapOf(validPath.name to value).asKVSource() }
            val sourceForKey by memoized { mapOf(validKey to value).asSource() }
            on("find a valid path") {
                it("should contain the value") {
                    assertTrue(sourceForPath.contains(validPath))
                }
            }
            on("find an invalid path") {
                it("should not contain the value") {
                    assertTrue(!sourceForPath.contains(invalidPath))
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
                    assertThat(sourceForPath.get(validPath), equalTo(value))
                }
            }
            on("get by an invalid path using `get`") {
                it("should throw NoSuchPathException") {
                    assertThat({ sourceForPath.get(invalidPath) },
                            throws(has(NoSuchPathException::path, equalTo(invalidPath))))
                }
            }

            on("find a valid key") {
                it("should contain the value") {
                    assertTrue(sourceForKey.contains(validKey))
                }
            }
            on("find an invalid key") {
                it("should not contain the value") {
                    assertTrue(!sourceForKey.contains(invalidKey))
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
                    assertThat(sourceForKey.get(validKey), equalTo(value))
                }
            }
            on("get by an invalid key using `get`") {
                it("should throw NoSuchPathException") {
                    assertThat({ sourceForKey.get(invalidKey) },
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
                val source = Int.MAX_VALUE.asSource()
                it("should throw ParseException") {
                    assertThat({ source.toShort() }, throws<ParseException>())
                }
            }

            on("cast int in range of byte to byte") {
                val source = 1.asSource()
                it("should succeed") {
                    assertThat(source.toByte(), equalTo(1.toByte()))
                }
            }
            on("cast int out of range of byte to byte") {
                val source = Int.MAX_VALUE.asSource()
                it("should throw ParseException") {
                    assertThat({ source.toByte() }, throws<ParseException>())
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
    }
})
