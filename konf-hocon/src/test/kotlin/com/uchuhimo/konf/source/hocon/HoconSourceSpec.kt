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

package com.uchuhimo.konf.source.hocon

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.uchuhimo.konf.source.asSource
import com.uchuhimo.konf.source.asValue
import com.uchuhimo.konf.toPath
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek
import kotlin.test.assertNull
import kotlin.test.assertTrue

object HoconSourceSpec : SubjectSpek<HoconSource>({
    subject { HoconProvider.string("key = 1") as HoconSource }

    given("a HOCON source") {
        on("get underlying config") {
            it("should return corresponding config") {
                val config = subject.value
                assertThat(config.getInt("key"), equalTo(1))
            }
        }
        on("get an existed key") {
            it("should contain the key") {
                assertTrue("key".toPath() in subject)
            }
            it("should contain the corresponding value") {
                assertThat(subject["key".toPath()].asValue<Int>(), equalTo(1))
            }
        }
        on("get an non-existed key") {
            it("should not contain the key") {
                assertTrue("invalid".toPath() !in subject)
            }
            it("should not contain the corresponding value") {
                assertNull(subject.getOrNull("invalid".toPath()))
            }
        }
        on("use substitutions in source") {
            val source = HoconProvider.string(
                """
                key1 = 1
                key2 = ${'$'}{key1}
                """.trimIndent()
            )
            it("should resolve the key") {
                assertThat(source["key2"].asValue<Int>(), equalTo(1))
            }
        }
        on("use substitutions in source when variables are in other sources") {
            val source = (
                HoconProvider.string(
                    """
                key1 = "1"
                key2 = ${'$'}{key1}
                key3 = "${'$'}{key4}"
                key5 = "${'$'}{key1}+${'$'}{key4}"
                key6 = "${"$$"}{key1}"
                    """.trimIndent()
                ) +
                    mapOf("key4" to "4", "key1" to "2").asSource()
                ).substituted().substituted()
            it("should resolve the key") {
                assertThat(source["key2"].asValue<Int>(), equalTo(1))
                assertThat(source["key3"].asValue<Int>(), equalTo(4))
                assertThat(source["key5"].asValue<String>(), equalTo("2+4"))
                assertThat(source["key6"].asValue<String>(), equalTo("\${key1}"))
            }
        }
    }
})
