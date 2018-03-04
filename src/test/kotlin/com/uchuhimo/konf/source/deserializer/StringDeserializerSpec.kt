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

package com.uchuhimo.konf.source.deserializer

import com.fasterxml.jackson.databind.DeserializationFeature
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import com.uchuhimo.konf.source.ObjectMappingException
import com.uchuhimo.konf.source.assertCausedBy
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

object StringDeserializerSpec : Spek({
    val spec = object : ConfigSpec() {
        val item by required<StringWrapper>()
    }
    val config by memoized {
        Config {
            addSpec(spec)
        }
    }

    given("a string deserializer") {
        on("deserialize string containing commas") {
            config.withSourceFrom.map.kv(mapOf("item" to mapOf("string" to "a,b,c"))).apply {
                it("should succeed") {
                    assertThat(this@apply[spec.item].string, equalTo("a,b,c"))
                }
            }
        }
        on("deserialize string containing commas when UNWRAP_SINGLE_VALUE_ARRAYS is enable") {
            config.apply {
                mapper.enable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
            }.withSourceFrom.map.kv(mapOf("item" to mapOf("string" to "a,b,c"))).apply {
                it("should succeed") {
                    assertThat(this@apply[spec.item].string, equalTo("a,b,c"))
                }
            }
        }
        on("deserialize string from number") {
            config.withSourceFrom.map.kv(mapOf("item" to mapOf("string" to 1))).apply {
                it("should succeed") {
                    assertThat(this@apply[spec.item].string, equalTo("1"))
                }
            }
        }
        on("deserialize string from list of numbers") {
            config.withSourceFrom.map.kv(mapOf("item" to mapOf("string" to listOf(1, 2)))).apply {
                it("should succeed") {
                    assertThat(this@apply[spec.item].string, equalTo("1,2"))
                }
            }
        }
        on("deserialize string from single value array") {
            config.apply {
                mapper.enable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
            }.withSourceFrom.map.kv(mapOf("item" to mapOf("string" to listOf("a")))).apply {
                it("should succeed") {
                    assertThat(this@apply[spec.item].string, equalTo("a"))
                }
            }
        }
        on("deserialize string from empty array") {
            it("should throw LoadException caused by ObjectMappingException") {
                assertCausedBy<ObjectMappingException> {
                    config.apply {
                        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT)
                    }.withSourceFrom.map.kv(mapOf("item" to mapOf("string" to listOf<String>())))
                }
            }
        }
    }
})

private data class StringWrapper(val string: String)
