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

package com.uchuhimo.konf.source.deserializer

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
import java.time.OffsetDateTime

object OffsetDateTimeDeserializerSpec : Spek({
    val spec = object : ConfigSpec() {
        val item by required<OffsetDateTimeWrapper>()
    }
    val config by memoized {
        Config {
            addSpec(spec)
        }
    }

    given("an OffsetDateTime deserializer") {
        on("deserialize valid string") {
            config.from.map.kv(mapOf("item" to mapOf("offsetDateTime" to "2007-12-03T10:15:30+01:00"))).apply {
                it("should succeed") {
                    assertThat(
                        this@apply[spec.item].offsetDateTime,
                        equalTo(OffsetDateTime.parse("2007-12-03T10:15:30+01:00"))
                    )
                }
            }
        }
        on("deserialize empty string") {
            it("should throw LoadException caused by ObjectMappingException") {
                assertCausedBy<ObjectMappingException> {
                    config.from.map.kv(mapOf("item" to mapOf("offsetDateTime" to "  ")))
                }
            }
        }
        on("deserialize value with invalid type") {
            it("should throw LoadException caused by ObjectMappingException") {
                assertCausedBy<ObjectMappingException> {
                    config.from.map.kv(mapOf("item" to mapOf("offsetDateTime" to 1)))
                }
            }
        }
        on("deserialize value with invalid format") {
            it("should throw LoadException caused by ObjectMappingException") {
                assertCausedBy<ObjectMappingException> {
                    config.from.map.kv(mapOf("item" to mapOf("offsetDateTime" to "2007-12-03T10:15:30")))
                }
            }
        }
    }
})

private data class OffsetDateTimeWrapper(val offsetDateTime: OffsetDateTime)
