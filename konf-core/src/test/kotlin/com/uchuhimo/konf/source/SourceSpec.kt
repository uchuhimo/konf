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

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.sameInstance
import com.natpryce.hamkrest.throws
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import com.uchuhimo.konf.Feature
import com.uchuhimo.konf.NetworkBuffer
import com.uchuhimo.konf.Prefix
import com.uchuhimo.konf.SizeInBytes
import com.uchuhimo.konf.ValueNode
import com.uchuhimo.konf.name
import com.uchuhimo.konf.source.base.ValueSource
import com.uchuhimo.konf.source.base.asKVSource
import com.uchuhimo.konf.source.base.toHierarchical
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
            val tree = value.tree
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
                    assertThat(sourceForPath.getOrNull(validPath)?.tree, equalTo(tree))
                }
            }
            on("get by an invalid path using `getOrNull`") {
                it("should return null") {
                    assertThat(sourceForPath.getOrNull(invalidPath), absent())
                }
            }

            on("get by a valid path using `get`") {
                it("should return the corresponding value") {
                    assertThat(sourceForPath[validPath].tree, equalTo(tree))
                }
            }
            on("get by an invalid path using `get`") {
                it("should throw NoSuchPathException") {
                    assertThat(
                        { sourceForPath[invalidPath] },
                        throws(has(NoSuchPathException::path, equalTo(invalidPath)))
                    )
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
                    assertThat(sourceForKey.getOrNull(validKey)?.tree, equalTo(tree))
                }
            }
            on("get by an invalid key using `getOrNull`") {
                it("should return null") {
                    assertThat(sourceForKey.getOrNull(invalidKey), absent())
                }
            }

            on("get by a valid key using `get`") {
                it("should return the corresponding value") {
                    assertThat(sourceForKey[validKey].tree, equalTo(tree))
                }
            }
            on("get by an invalid key using `get`") {
                it("should throw NoSuchPathException") {
                    assertThat(
                        { sourceForKey[invalidKey] },
                        throws(has(NoSuchPathException::path, equalTo(invalidKey.toPath())))
                    )
                }
            }
        }
        group("cast operation") {
            on("cast int to long") {
                val source = 1.asSource()
                it("should succeed") {
                    assertThat(source.asValue<Long>(), equalTo(1L))
                }
            }
            on("cast short to int") {
                val source = 1.toShort().asSource()
                it("should succeed") {
                    assertThat(source.asValue<Int>(), equalTo(1))
                }
            }
            on("cast byte to short") {
                val source = 1.toByte().asSource()
                it("should succeed") {
                    assertThat(source.asValue<Short>(), equalTo(1.toShort()))
                }
            }
            on("cast long to BigInteger") {
                val source = 1L.asSource()
                it("should succeed") {
                    assertThat(source.asValue<BigInteger>(), equalTo(BigInteger.valueOf(1)))
                }
            }

            on("cast double to BigDecimal") {
                val source = 1.5.asSource()
                it("should succeed") {
                    assertThat(source.asValue<BigDecimal>(), equalTo(BigDecimal.valueOf(1.5)))
                }
            }

            on("cast long in range of int to int") {
                val source = 1L.asSource()
                it("should succeed") {
                    assertThat(source.asValue<Int>(), equalTo(1))
                }
            }
            on("cast long out of range of int to int") {
                it("should throw ParseException") {
                    assertThat({ Long.MAX_VALUE.asSource().asValue<Int>() }, throws<ParseException>())
                    assertThat({ Long.MIN_VALUE.asSource().asValue<Int>() }, throws<ParseException>())
                }
            }

            on("cast int in range of short to short") {
                val source = 1.asSource()
                it("should succeed") {
                    assertThat(source.asValue<Short>(), equalTo(1.toShort()))
                }
            }
            on("cast int out of range of short to short") {
                it("should throw ParseException") {
                    assertThat({ Int.MAX_VALUE.asSource().asValue<Short>() }, throws<ParseException>())
                    assertThat({ Int.MIN_VALUE.asSource().asValue<Short>() }, throws<ParseException>())
                }
            }

            on("cast short in range of byte to byte") {
                val source = 1.toShort().asSource()
                it("should succeed") {
                    assertThat(source.asValue<Byte>(), equalTo(1.toByte()))
                }
            }
            on("cast short out of range of byte to byte") {
                it("should throw ParseException") {
                    assertThat({ Short.MAX_VALUE.asSource().asValue<Byte>() }, throws<ParseException>())
                    assertThat({ Short.MIN_VALUE.asSource().asValue<Byte>() }, throws<ParseException>())
                }
            }

            on("cast long in range of byte to byte") {
                val source = 1L.asSource()
                it("should succeed") {
                    assertThat(source.asValue<Byte>(), equalTo(1L.toByte()))
                }
            }
            on("cast long out of range of byte to byte") {
                it("should throw ParseException") {
                    assertThat({ Long.MAX_VALUE.asSource().asValue<Byte>() }, throws<ParseException>())
                    assertThat({ Long.MIN_VALUE.asSource().asValue<Byte>() }, throws<ParseException>())
                }
            }

            on("cast double to float") {
                val source = 1.5.asSource()
                it("should succeed") {
                    assertThat(source.asValue<Float>(), equalTo(1.5f))
                }
            }

            on("cast char to string") {
                val source = 'a'.asSource()
                it("should succeed") {
                    assertThat(source.asValue<String>(), equalTo("a"))
                }
            }
            on("cast string containing single char to char") {
                val source = "a".asSource()
                it("should succeed") {
                    assertThat(source.asValue<Char>(), equalTo('a'))
                }
            }
            on("cast string containing multiple chars to char") {
                val source = "ab".asSource()
                it("should throw ParseException") {
                    assertThat({ source.asValue<Char>() }, throws<ParseException>())
                }
            }

            on("cast \"true\" to Boolean") {
                val source = "true".asSource()
                it("should succeed") {
                    assertTrue { source.asValue() }
                }
            }
            on("cast \"false\" to Boolean") {
                val source = "false".asSource()
                it("should succeed") {
                    assertFalse { source.asValue() }
                }
            }
            on("cast string with invalid format to Boolean") {
                val source = "yes".asSource()
                it("should throw ParseException") {
                    assertThat({ source.asValue<Boolean>() }, throws<ParseException>())
                }
            }

            on("cast string to Byte") {
                val source = "1".asSource()
                it("should succeed") {
                    assertThat(source.asValue<Byte>(), equalTo(1.toByte()))
                }
            }
            on("cast string to Short") {
                val source = "1".asSource()
                it("should succeed") {
                    assertThat(source.asValue<Short>(), equalTo(1.toShort()))
                }
            }
            on("cast string to Int") {
                val source = "1".asSource()
                it("should succeed") {
                    assertThat(source.asValue<Int>(), equalTo(1))
                }
            }
            on("cast string to Long") {
                val source = "1".asSource()
                it("should succeed") {
                    assertThat(source.asValue<Long>(), equalTo(1L))
                }
            }
            on("cast string to Float") {
                val source = "1.5".asSource()
                it("should succeed") {
                    assertThat(source.asValue<Float>(), equalTo(1.5F))
                }
            }
            on("cast string to Double") {
                val source = "1.5F".asSource()
                it("should succeed") {
                    assertThat(source.asValue<Double>(), equalTo(1.5))
                }
            }
            on("cast string to BigInteger") {
                val source = "1".asSource()
                it("should succeed") {
                    assertThat(source.asValue<BigInteger>(), equalTo(1.toBigInteger()))
                }
            }
            on("cast string to BigDecimal") {
                val source = "1.5".asSource()
                it("should succeed") {
                    assertThat(source.asValue<BigDecimal>(), equalTo(1.5.toBigDecimal()))
                }
            }

            on("cast string to OffsetTime") {
                val text = "10:15:30+01:00"
                val source = text.asSource()
                it("should succeed") {
                    assertThat(source.asValue<OffsetTime>(), equalTo(OffsetTime.parse(text)))
                }
            }
            on("cast string with invalid format to OffsetTime") {
                val text = "10:15:30"
                val source = text.asSource()
                it("should throw ParseException") {
                    assertThat({ source.asValue<OffsetTime>() }, throws<ParseException>())
                }
            }

            on("cast string to OffsetDateTime") {
                val text = "2007-12-03T10:15:30+01:00"
                val source = text.asSource()
                it("should succeed") {
                    assertThat(source.asValue<OffsetDateTime>(), equalTo(OffsetDateTime.parse(text)))
                }
            }

            on("cast string to ZonedDateTime") {
                val text = "2007-12-03T10:15:30+01:00[Europe/Paris]"
                val source = text.asSource()
                it("should succeed") {
                    assertThat(source.asValue<ZonedDateTime>(), equalTo(ZonedDateTime.parse(text)))
                }
            }

            on("cast string to LocalDate") {
                val text = "2007-12-03"
                val source = text.asSource()
                it("should succeed") {
                    assertThat(source.asValue<LocalDate>(), equalTo(LocalDate.parse(text)))
                }
            }

            on("cast string to LocalTime") {
                val text = "10:15:30"
                val source = text.asSource()
                it("should succeed") {
                    assertThat(source.asValue<LocalTime>(), equalTo(LocalTime.parse(text)))
                }
            }

            on("cast string to LocalDateTime") {
                val text = "2007-12-03T10:15:30"
                val source = text.asSource()
                it("should succeed") {
                    assertThat(source.asValue<LocalDateTime>(), equalTo(LocalDateTime.parse(text)))
                }
            }

            on("cast string to Year") {
                val text = "2007"
                val source = text.asSource()
                it("should succeed") {
                    assertThat(source.asValue<Year>(), equalTo(Year.parse(text)))
                }
            }

            on("cast string to YearMonth") {
                val text = "2007-12"
                val source = text.asSource()
                it("should succeed") {
                    assertThat(source.asValue<YearMonth>(), equalTo(YearMonth.parse(text)))
                }
            }

            on("cast string to Instant") {
                val text = "2007-12-03T10:15:30.00Z"
                val source = text.asSource()
                it("should succeed") {
                    assertThat(source.asValue<Instant>(), equalTo(Instant.parse(text)))
                }
            }

            on("cast string to Date") {
                val text = "2007-12-03T10:15:30.00Z"
                val source = text.asSource()
                it("should succeed") {
                    assertThat(source.asValue<Date>(), equalTo(Date.from(Instant.parse(text))))
                }
            }

            on("cast LocalDateTime string to Date") {
                val text = "2007-12-03T10:15:30"
                val source = text.asSource()
                it("should succeed") {
                    assertThat(
                        source.asValue<Date>(),
                        equalTo(Date.from(LocalDateTime.parse(text).toInstant(ZoneOffset.UTC)))
                    )
                }
            }

            on("cast LocalDate string to Date") {
                val text = "2007-12-03"
                val source = text.asSource()
                it("should succeed") {
                    assertThat(
                        source.asValue<Date>(),
                        equalTo(Date.from(LocalDate.parse(text).atStartOfDay().toInstant(ZoneOffset.UTC)))
                    )
                }
            }

            on("cast string to Duration") {
                val text = "P2DT3H4M"
                val source = text.asSource()
                it("should succeed") {
                    assertThat(source.asValue<Duration>(), equalTo(Duration.parse(text)))
                }
            }

            on("cast string with simple unit to Duration") {
                val text = "200ms"
                val source = text.asSource()
                it("should succeed") {
                    assertThat(source.asValue<Duration>(), equalTo(Duration.ofMillis(200)))
                }
            }
            on("cast string with invalid format to Duration") {
                val text = "2 year"
                val source = text.asSource()
                it("should throw ParseException") {
                    assertThat({ source.asValue<Duration>() }, throws<ParseException>())
                }
            }

            on("cast string to SizeInBytes") {
                val text = "10k"
                val source = text.asSource()
                it("should succeed") {
                    assertThat(source.asValue<SizeInBytes>().bytes, equalTo(10240L))
                }
            }
            on("cast string with invalid format to SizeInBytes") {
                val text = "10u"
                val source = text.asSource()
                it("should throw ParseException") {
                    assertThat({ source.asValue<SizeInBytes>() }, throws<ParseException>())
                }
            }

            on("cast set to list") {
                val source = setOf(1).asSource()
                it("should succeed") {
                    assertThat(source.asValue<List<Int>>(), equalTo(listOf(1)))
                }
            }
            on("cast array to list") {
                val source = arrayOf(1).asSource()
                it("should succeed") {
                    assertThat(source.asValue<List<Int>>(), equalTo(listOf(1)))
                }
            }
            on("cast array to set") {
                val source = arrayOf(1).asSource()
                it("should succeed") {
                    assertThat(source.asValue<Set<Int>>(), equalTo(setOf(1)))
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
                        load<Map<Pair<Int, Int>, String>>(mapOf((1 to 1) to "1"))
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
                it("should throw LoadException caused by ObjectMappingException") {
                    assertCausedBy<ObjectMappingException> {
                        load<Person>(mapOf("name" to Source()))
                    }
                }
            }
            on("load when SUBSTITUTE_SOURCE_WHEN_LOADED is disabled on config") {
                val source = mapOf("item" to mapOf("key1" to "a", "key2" to "b\${item.key1}")).asSource()
                val config = Config {
                    addSpec(
                        object : ConfigSpec() {
                            @Suppress("unused")
                            val item by required<Map<String, String>>()
                        }
                    )
                }.disable(Feature.SUBSTITUTE_SOURCE_BEFORE_LOADED)
                    .withSource(source)
                it("should not substitute path variables before loaded") {
                    assertThat(
                        config<Map<String, String>>("item"),
                        equalTo(mapOf("key1" to "a", "key2" to "b\${item.key1}"))
                    )
                }
            }
            on("load when SUBSTITUTE_SOURCE_WHEN_LOADED is disabled on source") {
                val source = mapOf("item" to mapOf("key1" to "a", "key2" to "b\${item.key1}")).asSource()
                    .disabled(Feature.SUBSTITUTE_SOURCE_BEFORE_LOADED)
                val config = Config {
                    addSpec(
                        object : ConfigSpec() {
                            @Suppress("unused")
                            val item by required<Map<String, String>>()
                        }
                    )
                }.withSource(source)
                it("should substitute path variables before loaded") {
                    assertThat(
                        config<Map<String, String>>("item"),
                        equalTo(mapOf("key1" to "a", "key2" to "b\${item.key1}"))
                    )
                }
            }
            on("load when SUBSTITUTE_SOURCE_WHEN_LOADED is enabled") {
                val source = mapOf("item" to mapOf("key1" to "a", "key2" to "b\${item.key1}")).asSource()
                val config = Config {
                    addSpec(
                        object : ConfigSpec() {
                            @Suppress("unused")
                            val item by required<Map<String, String>>()
                        }
                    )
                }.withSource(source)
                it("should substitute path variables") {
                    assertTrue { Feature.SUBSTITUTE_SOURCE_BEFORE_LOADED.enabledByDefault }
                    assertThat(
                        config<Map<String, String>>("item"),
                        equalTo(mapOf("key1" to "a", "key2" to "ba"))
                    )
                }
            }
        }
        group("substitution operation") {
            on("doesn't contain any path variable") {
                val map = mapOf("key1" to "a", "key2" to "b")
                val source = map.asSource().substituted()
                it("should keep it unchanged") {
                    assertThat(source.tree.toHierarchical(), equalTo<Any>(map))
                }
            }
            on("contains single path variable") {
                val map = mapOf("key1" to "a", "key2" to "b\${key1}")
                val source = map.asSource().substituted()
                it("should substitute path variables") {
                    assertThat(
                        source.tree.toHierarchical(),
                        equalTo<Any>(mapOf("key1" to "a", "key2" to "ba"))
                    )
                }
            }
            on("contains integer path variable") {
                val map = mapOf("key1" to 1, "key2" to "b\${key1}", "key3" to "\${key1}")
                val source = map.asSource().substituted()
                it("should substitute path variables") {
                    assertThat(
                        source.tree.toHierarchical(),
                        equalTo<Any>(mapOf("key1" to 1, "key2" to "b1", "key3" to 1))
                    )
                }
            }
            on("contains path variables with string list value") {
                val map = mapOf("key1" to "a,b,c", "key2" to "a\${key1}")
                val source = Source.from.map.flat(map).substituted().substituted()
                it("should substitute path variables") {
                    assertThat(
                        source.tree.toHierarchical(),
                        equalTo<Any>(
                            mapOf(
                                "key1" to "a,b,c",
                                "key2" to "aa,b,c"
                            )
                        )
                    )
                }
            }
            on("contains path variables in list") {
                val map = mapOf("top" to listOf(mapOf("key1" to "a", "key2" to "b\${top.0.key1}")))
                val source = map.asSource().substituted()
                it("should substitute path variables") {
                    assertThat(
                        source.tree.toHierarchical(),
                        equalTo<Any>(mapOf("top" to listOf(mapOf("key1" to "a", "key2" to "ba"))))
                    )
                }
            }
            on("contains path variable with wrong type") {
                val map = mapOf("key1" to 1.0, "key2" to "b\${key1}")
                it("should throw WrongTypeException") {
                    assertThrows<WrongTypeException> { map.asSource().substituted() }
                }
            }
            on("contains escaped path variables") {
                val map = mapOf("key1" to "a", "key2" to "b\$\${key1}")
                val source = map.asSource().substituted()
                it("should not substitute path variables") {
                    assertThat(
                        source.tree.toHierarchical(),
                        equalTo<Any>(mapOf("key1" to "a", "key2" to "b\${key1}"))
                    )
                }
            }
            on("contains nested escaped path variables") {
                val map = mapOf("key1" to "a", "key2" to "b\$\$\$\${key1}")
                val source = map.asSource().substituted()
                it("should escaped only once") {
                    assertThat(
                        source.tree.toHierarchical(),
                        equalTo<Any>(mapOf("key1" to "a", "key2" to "b\$\$\${key1}"))
                    )
                }
            }
            on("contains nested escaped path variables and substitute multiple times") {
                val map = mapOf("key1" to "a", "key2" to "b\$\$\$\${key1}")
                val source = map.asSource().substituted().substituted()
                it("should escaped only once") {
                    assertThat(
                        source.tree.toHierarchical(),
                        equalTo<Any>(mapOf("key1" to "a", "key2" to "b\$\$\${key1}"))
                    )
                }
            }
            on("contains undefined path variable") {
                val map = mapOf("key2" to "b\${key1}")
                it("should throw UndefinedPathVariableException by default") {
                    assertThat(
                        { map.asSource().substituted() },
                        throws(has(UndefinedPathVariableException::text, equalTo("b\${key1}")))
                    )
                }
                it("should keep unsubstituted when errorWhenUndefined is `false`") {
                    val source = map.asSource().substituted(errorWhenUndefined = false)
                    assertThat(source.tree.toHierarchical(), equalTo<Any>(map))
                }
            }
            on("contains undefined path variable in reference format") {
                val map = mapOf("key2" to "\${key1}")
                it("should throw UndefinedPathVariableException by default") {
                    assertThat(
                        { map.asSource().substituted() },
                        throws(has(UndefinedPathVariableException::text, equalTo("\${key1}")))
                    )
                }
                it("should keep unsubstituted when errorWhenUndefined is `false`") {
                    val source = map.asSource().substituted(errorWhenUndefined = false)
                    assertThat(source.tree.toHierarchical(), equalTo<Any>(map))
                }
            }
            on("contains multiple path variables") {
                val map = mapOf("key1" to "a", "key2" to "\${key1}b\${key3}", "key3" to "c")
                val source = map.asSource().substituted()
                it("should substitute path variables") {
                    assertThat(
                        source.tree.toHierarchical(),
                        equalTo<Any>(mapOf("key1" to "a", "key2" to "abc", "key3" to "c"))
                    )
                }
            }
            on("contains chained path variables") {
                val map = mapOf("key1" to "a", "key2" to "\${key1}b", "key3" to "\${key2}c")
                val source = map.asSource().substituted()
                it("should substitute path variables") {
                    assertThat(
                        source.tree.toHierarchical(),
                        equalTo<Any>(mapOf("key1" to "a", "key2" to "ab", "key3" to "abc"))
                    )
                }
            }
            on("contains nested path variables") {
                val map = mapOf("key1" to "a", "key2" to "\${\${key3}}b", "key3" to "key1")
                val source = map.asSource().substituted()
                it("should substitute path variables") {
                    assertThat(
                        source.tree.toHierarchical(),
                        equalTo<Any>(mapOf("key1" to "a", "key2" to "ab", "key3" to "key1"))
                    )
                }
            }
            on("contains a path variable with default value") {
                val map = mapOf("key1" to "a", "key2" to "b\${key3:-c}")
                val source = map.asSource().substituted()
                it("should substitute path variables") {
                    assertThat(
                        source.tree.toHierarchical(),
                        equalTo<Any>(mapOf("key1" to "a", "key2" to "bc"))
                    )
                }
            }
            on("contains a path variable with key") {
                val map = mapOf("key1" to "a", "key2" to "\${key1}\${base64Decoder:SGVsbG9Xb3JsZCE=}")
                val source = map.asSource().substituted()
                it("should substitute path variables") {
                    assertThat(
                        source.tree.toHierarchical(),
                        equalTo<Any>(mapOf("key1" to "a", "key2" to "aHelloWorld!"))
                    )
                }
            }
            on("contains a path variable in reference format") {
                val map = mapOf("key1" to mapOf("key3" to "a", "key4" to "b"), "key2" to "\${key1}")
                val source = map.asSource().substituted()
                it("should substitute path variables") {
                    assertThat(
                        source.tree.toHierarchical(),
                        equalTo<Any>(
                            mapOf(
                                "key1" to mapOf("key3" to "a", "key4" to "b"),
                                "key2" to mapOf("key3" to "a", "key4" to "b")
                            )
                        )
                    )
                }
            }
            on("contains nested path variable in reference format") {
                val map = mapOf("key1" to mapOf("key3" to "a", "key4" to "b"), "key2" to "\${\${key3}}", "key3" to "key1")
                val source = map.asSource().substituted()
                it("should substitute path variables") {
                    assertThat(
                        source.tree.toHierarchical(),
                        equalTo<Any>(
                            mapOf(
                                "key1" to mapOf("key3" to "a", "key4" to "b"),
                                "key2" to mapOf("key3" to "a", "key4" to "b"),
                                "key3" to "key1"
                            )
                        )
                    )
                }
            }
            on("contains path variable in different sources") {
                val map1 = mapOf("key1" to "a")
                val map2 = mapOf("key2" to "b\${key1}")
                val source = (map2.asSource() + map1.asSource()).substituted()
                it("should substitute path variables") {
                    assertThat(
                        source.tree.toHierarchical(),
                        equalTo<Any>(mapOf("key1" to "a", "key2" to "ba"))
                    )
                }
            }
        }
        group("feature operation") {
            on("enable feature") {
                val source = Source().enabled(Feature.FAIL_ON_UNKNOWN_PATH)
                it("should let the feature be enabled") {
                    assertTrue { source.isEnabled(Feature.FAIL_ON_UNKNOWN_PATH) }
                }
            }
            on("disable feature") {
                val source = Source().disabled(Feature.FAIL_ON_UNKNOWN_PATH)
                it("should let the feature be disabled") {
                    assertFalse { source.isEnabled(Feature.FAIL_ON_UNKNOWN_PATH) }
                }
            }
            on("enable feature before transforming source") {
                val source = Source().enabled(Feature.FAIL_ON_UNKNOWN_PATH).withPrefix("prefix")
                it("should let the feature be enabled") {
                    assertTrue { source.isEnabled(Feature.FAIL_ON_UNKNOWN_PATH) }
                }
            }
            on("disable feature before transforming source") {
                val source = Source().disabled(Feature.FAIL_ON_UNKNOWN_PATH).withPrefix("prefix")
                it("should let the feature be disabled") {
                    assertFalse { source.isEnabled(Feature.FAIL_ON_UNKNOWN_PATH) }
                }
            }
            on("by default") {
                val source = Source()
                it("should use the feature's default setting") {
                    assertThat(
                        source.isEnabled(Feature.FAIL_ON_UNKNOWN_PATH),
                        equalTo(Feature.FAIL_ON_UNKNOWN_PATH.enabledByDefault)
                    )
                }
            }
        }
        group("source with prefix") {
            val source by memoized {
                Prefix("level1.level2") + mapOf("key" to "value").asSource()
            }
            on("prefix is empty") {
                it("should return itself") {
                    assertThat(source.withPrefix(""), sameInstance(source))
                }
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
                        (source.getOrNull("level1")?.get("level2.key")?.tree as ValueNode).value as String,
                        equalTo("value")
                    )
                    assertThat(
                        (source.getOrNull("level1.level2")?.get("key")?.tree as ValueNode).value as String,
                        equalTo("value")
                    )
                    assertThat(
                        (source.getOrNull("level1.level2.key")?.tree as ValueNode).value as String,
                        equalTo("value")
                    )
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
        addSpec(
            object : ConfigSpec() {
                @Suppress("unused")
                val item by required<T>()
            }
        )
    }.withSource(mapOf("item" to value).asSource())

private data class Person(val name: String)
