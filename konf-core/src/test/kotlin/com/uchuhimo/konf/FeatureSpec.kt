/*
 * Copyright 2017-2020 the original author or authors.
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

package com.uchuhimo.konf

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.throws
import com.uchuhimo.konf.source.UnknownPathsException
import com.uchuhimo.konf.source.asSource
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.junit.jupiter.api.assertThrows
import java.io.FileNotFoundException

object FailOnUnknownPathSpec : Spek({
    //language=Json
    val source =
        """
        {
            "level1": {
              "level2": {
                "valid": "value1",
                "invalid": "value2"
              }
            }
        }
        """.trimIndent()
    given("a config") {
        on("the feature is disabled") {
            val config = Config {
                addSpec(Valid)
            }
            it("should ignore unknown paths") {
                val conf = config.from.disabled(Feature.FAIL_ON_UNKNOWN_PATH).json.string(source)
                assertThat(conf[Valid.valid], equalTo("value1"))
            }
        }
        on("the feature is enabled on config") {
            val config = Config {
                addSpec(Valid)
            }.enable(Feature.FAIL_ON_UNKNOWN_PATH)
            it("should throws UnknownPathsException and reports the unknown paths") {
                assertThat(
                    { config.from.json.string(source) },
                    throws(
                        has(
                            UnknownPathsException::paths,
                            equalTo(listOf("level1.level2.invalid"))
                        )
                    )
                )
            }
        }
        on("the feature is enabled on source") {
            val config = Config {
                addSpec(Valid)
            }
            it("should throws UnknownPathsException and reports the unknown paths") {
                assertThat(
                    {
                        config.from.enabled(Feature.FAIL_ON_UNKNOWN_PATH).json.string(source)
                    },
                    throws(
                        has(
                            UnknownPathsException::paths,
                            equalTo(listOf("level1.level2.invalid"))
                        )
                    )
                )
            }
        }
    }
})

object LoadKeysCaseInsensitivelySpec : Spek({
    given("a config") {
        on("by default") {
            val source = mapOf("somekey" to "value").asSource()
            val config = Config().withSource(source)
            it("should load keys case-sensitively") {
                val someKey by config.required<String>()
                assertThrows<UnsetValueException> { someKey.isNotEmpty() }
                val somekey by config.required<String>()
                assertThat(somekey, equalTo("value"))
            }
        }
        on("the feature is disabled") {
            val source = mapOf("somekey" to "value").asSource().disabled(Feature.LOAD_KEYS_CASE_INSENSITIVELY)
            val config = Config().withSource(source)
            it("should load keys case-sensitively") {
                val someKey by config.required<String>()
                assertThrows<UnsetValueException> { someKey.isNotEmpty() }
                val somekey by config.required<String>()
                assertThat(somekey, equalTo("value"))
            }
        }
        on("the feature is enabled on config") {
            val source = mapOf("somekey" to "value").asSource()
            val config = Config().enable(Feature.LOAD_KEYS_CASE_INSENSITIVELY).withSource(source)
            it("should load keys case-insensitively") {
                val someKey by config.required<String>()
                assertThat(someKey, equalTo("value"))
            }
        }
        on("the feature is enabled on source") {
            val source = mapOf("somekey" to "value").asSource().enabled(Feature.LOAD_KEYS_CASE_INSENSITIVELY)
            val config = Config().withSource(source)
            it("should load keys case-insensitively") {
                val someKey by config.required<String>()
                assertThat(someKey, equalTo("value"))
            }
        }
    }
})

object LoadKeysAsLittleCamelCaseSpec : Spek({
    given("a config") {
        on("by default") {
            val source = mapOf(
                "some_key" to "value",
                "some_key2_" to "value",
                "_some_key3" to "value",
                "SomeKey4" to "value",
                "some_0key5" to "value",
                "some__key6" to "value",
                "some___key7" to "value",
                "some_some_key8" to "value",
                "some key9" to "value",
                "SOMEKey10" to "value"
            ).asSource()
            val config = Config().withSource(source)
            it("should load keys as little camel case") {
                val someKey by config.required<String>()
                assertThat(someKey, equalTo("value"))
                val someKey2 by config.required<String>()
                assertThat(someKey2, equalTo("value"))
                val someKey3 by config.required<String>()
                assertThat(someKey3, equalTo("value"))
                val someKey4 by config.required<String>()
                assertThat(someKey4, equalTo("value"))
                val some0key5 by config.required<String>()
                assertThat(some0key5, equalTo("value"))
                val someKey6 by config.required<String>()
                assertThat(someKey6, equalTo("value"))
                val someKey7 by config.required<String>()
                assertThat(someKey7, equalTo("value"))
                val someSomeKey8 by config.required<String>()
                assertThat(someSomeKey8, equalTo("value"))
                val someKey9 by config.required<String>()
                assertThat(someKey9, equalTo("value"))
                val someKey10 by config.required<String>()
                assertThat(someKey10, equalTo("value"))
            }
        }
        on("the feature is enabled") {
            val source = mapOf("some_key" to "value").asSource().enabled(Feature.LOAD_KEYS_AS_LITTLE_CAMEL_CASE)
            val config = Config().withSource(source)
            it("should load keys as little camel case") {
                val someKey by config.required<String>()
                assertThat(someKey, equalTo("value"))
            }
        }
        on("the feature is disabled on config") {
            val source = mapOf("some_key" to "value").asSource()
            val config = Config().disable(Feature.LOAD_KEYS_AS_LITTLE_CAMEL_CASE).withSource(source)
            it("should load keys as usual") {
                val someKey by config.required<String>()
                assertThrows<UnsetValueException> { someKey.isNotEmpty() }
                val some_key by config.required<String>()
                assertThat(some_key, equalTo("value"))
            }
        }
        on("the feature is disabled on source") {
            val source = mapOf("some_key" to "value").asSource().disabled(Feature.LOAD_KEYS_AS_LITTLE_CAMEL_CASE)
            val config = Config().withSource(source)
            it("should load keys as usual") {
                val someKey by config.required<String>()
                assertThrows<UnsetValueException> { someKey.isNotEmpty() }
                val some_key by config.required<String>()
                assertThat(some_key, equalTo("value"))
            }
        }
    }
})

object OptionalSourceByDefautSpec : Spek({
    given("a config") {
        on("the feature is disabled") {
            val config = Config().disable(Feature.OPTIONAL_SOURCE_BY_DEFAULT)
            it("should throw exception when file is not existed") {
                assertThrows<FileNotFoundException> { config.from.file("not_existed.json") }
            }
        }
        on("the feature is enabled on config") {
            val config = Config().enable(Feature.OPTIONAL_SOURCE_BY_DEFAULT)
            it("should load empty source") {
                config.from.mapped {
                    assertThat(it.tree.children, equalTo(mutableMapOf()))
                    it
                }.file("not_existed.json")
                config.from.mapped {
                    assertThat(it.tree.children, equalTo(mutableMapOf()))
                    it
                }.json.file("not_existed.json")
            }
        }
    }
})

private object Valid : ConfigSpec("level1.level2") {
    val valid by required<String>()
}
