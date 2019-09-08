/*
 * Copyright 2017-2019 the original author or authors.
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

package com.uchuhimo.konf.source.json

import com.fasterxml.jackson.databind.node.BigIntegerNode
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.DecimalNode
import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.FloatNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.LongNode
import com.fasterxml.jackson.databind.node.ShortNode
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import com.uchuhimo.konf.source.WrongTypeException
import com.uchuhimo.konf.source.asValue
import com.uchuhimo.konf.toPath
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.assertNull
import kotlin.test.assertTrue

object JsonSourceSpec : Spek({
    given("a JSON source") {
        group("get operation") {
            val source by memoized { JsonProvider.string("""{ "key": 1 }""") }
            on("get underlying JSON node") {
                val intSource = JsonSource(IntNode.valueOf(1))
                it("should return corresponding node") {
                    val node = intSource.node
                    assertTrue(node.isInt)
                    assertThat(node.intValue(), equalTo(1))
                }
            }
            on("get an existed key") {
                it("should contain the key") {
                    assertTrue("key".toPath() in source)
                }
                it("should contain the corresponding value") {
                    assertThat(source["key".toPath()].asValue<Int>(), equalTo(1))
                }
            }
            on("get an non-existed key") {
                it("should not contain the key") {
                    assertTrue("invalid".toPath() !in source)
                }
                it("should not contain the corresponding value") {
                    assertNull(source.getOrNull("invalid".toPath()))
                }
            }
        }
        group("cast operation") {
            on("get string from other source") {
                it("should throw WrongTypeException") {
                    assertThat({ JsonSource(IntNode.valueOf(1)).asValue<String>() }, throws<WrongTypeException>())
                }
            }
            on("get boolean from other source") {
                it("should throw WrongTypeException") {
                    assertThat({ JsonSource(IntNode.valueOf(1)).asValue<Boolean>() }, throws<WrongTypeException>())
                }
            }
            on("get double from other source") {
                it("should throw WrongTypeException") {
                    assertThat({ JsonSource(BooleanNode.valueOf(true)).asValue<Double>() }, throws<WrongTypeException>())
                }
            }
            on("get integer from other source") {
                it("should throw WrongTypeException") {
                    assertThat({ JsonSource(DoubleNode.valueOf(1.0)).asValue<Int>() }, throws<WrongTypeException>())
                }
            }
            on("get long from long source") {
                it("should succeed") {
                    assertThat(JsonSource(LongNode.valueOf(1L)).asValue<Long>(), equalTo(1L))
                }
            }
            on("get long from integer source") {
                it("should succeed") {
                    assertThat(JsonSource(IntNode.valueOf(1)).asValue<Long>(), equalTo(1L))
                }
            }
            on("get short from short source") {
                it("should succeed") {
                    assertThat(JsonSource(ShortNode.valueOf(1)).asValue<Short>(), equalTo(1.toShort()))
                }
            }
            on("get short from integer source") {
                it("should succeed") {
                    assertThat(JsonSource(IntNode.valueOf(1)).asValue<Short>(), equalTo(1.toShort()))
                }
            }
            on("get float from float source") {
                it("should succeed") {
                    assertThat(JsonSource(FloatNode.valueOf(1.0F)).asValue<Float>(), equalTo(1.0F))
                }
            }
            on("get float from double source") {
                it("should succeed") {
                    assertThat(JsonSource(DoubleNode.valueOf(1.0)).asValue<Float>(), equalTo(1.0F))
                }
            }
            on("get BigInteger from BigInteger source") {
                it("should succeed") {
                    assertThat(JsonSource(BigIntegerNode.valueOf(BigInteger.valueOf(1L))).asValue<BigInteger>(),
                        equalTo(BigInteger.valueOf(1L)))
                }
            }
            on("get BigInteger from long source") {
                it("should succeed") {
                    assertThat(JsonSource(LongNode.valueOf(1L)).asValue<BigInteger>(),
                        equalTo(BigInteger.valueOf(1L)))
                }
            }
            on("get BigDecimal from BigDecimal source") {
                it("should succeed") {
                    assertThat(JsonSource(DecimalNode.valueOf(BigDecimal.valueOf(1.0))).asValue<BigDecimal>(),
                        equalTo(BigDecimal.valueOf(1.0)))
                }
            }
            on("get BigDecimal from double source") {
                it("should succeed") {
                    assertThat(JsonSource(DoubleNode.valueOf(1.0)).asValue<BigDecimal>(),
                        equalTo(BigDecimal.valueOf(1.0)))
                }
            }
        }
    }
})
