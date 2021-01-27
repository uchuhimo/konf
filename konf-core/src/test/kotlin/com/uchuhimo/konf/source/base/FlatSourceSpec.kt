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

package com.uchuhimo.konf.source.base

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import com.uchuhimo.konf.InvalidPathException
import com.uchuhimo.konf.ListNode
import com.uchuhimo.konf.ValueNode
import com.uchuhimo.konf.source.ParseException
import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.asValue
import com.uchuhimo.konf.toPath
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertTrue

object FlatSourceSpec : SubjectSpek<FlatSource>({
    given("a flat map source") {
        group("get operation") {
            val source by memoized {
                FlatSource(map = mapOf("level1.level2.key" to "value"))
            }
            on("get the underlying map") {
                it("should return the specified map") {
                    assertThat(source.map, equalTo(mapOf("level1.level2.key" to "value")))
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
        group("get operation for list value") {
            val source by memoized {
                FlatSource(
                    map = mapOf(
                        "empty" to "",
                        "single" to "a",
                        "multiple" to "a,b"
                    )
                )
            }
            on("empty string value") {
                it("should return an empty list") {
                    assertThat((source["empty"].tree as ListNode).list, equalTo(listOf()))
                }
            }
            on("string value without commas") {
                it("should return a list containing a single element") {
                    assertThat(
                        (source["single"].tree as ListNode).list.map { (it as ValueNode).value as String },
                        equalTo(listOf("a"))
                    )
                }
            }
            on("string value with commas") {
                it("should return a list containing multiple elements") {
                    assertThat(
                        (source["multiple"].tree as ListNode).list.map { (it as ValueNode).value as String },
                        equalTo(listOf("a", "b"))
                    )
                }
            }
        }
        on("contain invalid key") {
            it("should throw InvalidPathException") {
                assertThrows<InvalidPathException> {
                    FlatSource(map = mapOf("level1.level2.key." to "value"))
                }
            }
        }
        group("cast operation") {
            val source by memoized {
                FlatSource(map = mapOf("level1.key" to "value"))["level1.key"]
            }
            on("value is a string") {
                it("should succeed to cast to string") {
                    assertThat(source.asValue<String>(), equalTo("value"))
                }
            }
            on("value is not a boolean") {
                it("should throw ParseException when casting to boolean") {
                    assertThat({ source.asValue<Boolean>() }, throws<ParseException>())
                }
            }
            on("value is not a double") {
                it("should throw ParseException when casting to double") {
                    assertThat({ source.asValue<Double>() }, throws<ParseException>())
                }
            }
            on("value is not an integer") {
                it("should throw ParseException when casting to integer") {
                    assertThat({ source.asValue<Int>() }, throws<ParseException>())
                }
            }
            on("value is not a long") {
                it("should throw ParseException when casting to long") {
                    assertThat({ source.asValue<Long>() }, throws<ParseException>())
                }
            }
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
