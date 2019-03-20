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

package com.uchuhimo.konf.source.base

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import com.uchuhimo.konf.source.NoSuchPathException
import com.uchuhimo.konf.source.ParseException
import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.toPath
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek
import kotlin.test.assertFalse
import kotlin.test.assertTrue

object FlatSourceSpec : SubjectSpek<FlatSource>({
    given("a flat map source") {
        group("get operation") {
            val source by memoized {
                FlatSource(map = mapOf("level1.level2.key" to "value"), prefix = "level1.level2")
            }
            on("get the underlying map") {
                it("should return the specified map") {
                    assertThat(source.map, equalTo(mapOf("level1.level2.key" to "value")))
                }
            }
            on("get prefix") {
                it("should return the specified prefix") {
                    assertThat(source.prefix, equalTo("level1.level2"))
                }
            }
            on("access with empty path") {
                it("should contain the path") {
                    assertTrue("".toPath() in source)
                }
                it("should return itself in `getOrNull`") {
                    assertThat(source.getOrNull("".toPath()), equalTo(source as Source))
                }
            }
        }
        on("contain invalid key") {
            val source = FlatSource(
                map = mapOf("level1.level2.key." to "value"),
                prefix = "level1.level2")
            it("should not contain any path") {
                assertFalse("key" in source)
            }
        }
        on("underlying map's key prefix mismatches with prefix") {
            val source = FlatSource(map = mapOf("level1.key" to "value"), prefix = "level2")
            it("should fail to cast to map") {
                assertFalse(source.isMap())
                assertTrue(source.toMap().isEmpty())
            }
        }
        on("underlying map's key mismatches with prefix") {
            val source = FlatSource(map = mapOf("level1" to "value"), prefix = "level2")
            it("should fail to cast to string") {
                assertFalse(source.isText())
                assertThat({ source.toText() }, throws<NoSuchPathException>())
            }
        }
        group("cast operation") {
            val source by memoized {
                FlatSource(map = mapOf("level1.key" to "value"), prefix = "level1.key")
            }
            on("value is a string") {
                it("should succeed to cast to string") {
                    assertTrue(source.isText())
                    assertThat(source.toText(), equalTo("value"))
                }
            }
            on("value is not a boolean") {
                it("should throw ParseException when casting to boolean") {
                    assertThat({ source.toBoolean() }, throws<ParseException>())
                }
            }
            on("value is not a double") {
                it("should throw ParseException when casting to double") {
                    assertThat({ source.toDouble() }, throws<ParseException>())
                }
            }
            on("value is not an integer") {
                it("should throw ParseException when casting to integer") {
                    assertThat({ source.toInt() }, throws<ParseException>())
                }
            }
            on("value is not a long") {
                it("should throw ParseException when casting to long") {
                    assertThat({ source.toLong() }, throws<ParseException>())
                }
            }
        }
    }

    given("a config that contains a required list of strings") {
        val parameterName = "flatsourcespeclist"
        val spec = object : ConfigSpec() {
            val list by required<List<String>>(name = parameterName)
        }
        fun configFromSystemProperties() = Config {
            addSpec(spec)
        }.from.systemProperties()
        it("should work with an empty list") {
            System.setProperty(parameterName, "")
            assertThat(configFromSystemProperties()[spec.list], equalTo(listOf()))
            System.clearProperty(parameterName)
        }
        it("should work with a single element") {
            System.setProperty(parameterName, "a")
            assertThat(configFromSystemProperties()[spec.list], equalTo(listOf("a")))
            System.clearProperty(parameterName)
        }
        it("should work with multiple elements") {
            System.setProperty(parameterName, "a,b")
            assertThat(configFromSystemProperties()[spec.list], equalTo(listOf("a", "b")))
            System.clearProperty(parameterName)
        }
    }
    given("a config that contains list of strings with commas") {
        val spec = object : ConfigSpec() {
            @Suppress("unused")
            val list by optional(listOf("a,b", "c, d"))
        }
        val config = Config {
            addSpec(spec)
        }
        val map = config.toFlatMap()
        it("should not be joined into a string") {
            assertThat(map["list.0"], equalTo("a,b"))
            assertThat(map["list.1"], equalTo("c, d"))
        }
    }
    given("a config that contains list of strings without commas") {
        val spec = object : ConfigSpec() {
            @Suppress("unused")
            val list by optional(listOf("a", "b", "c", "d"))
        }
        val config = Config {
            addSpec(spec)
        }
        val map = config.toFlatMap()
        it("should be joined into a string with commas") {
            assertThat(map["list"], equalTo("a,b,c,d"))
        }
    }
})
