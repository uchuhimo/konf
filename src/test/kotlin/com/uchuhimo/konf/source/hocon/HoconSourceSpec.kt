/*
 * Copyright 2017 the original author or authors.
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
import com.uchuhimo.konf.toPath
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

object HoconSourceSpec : SubjectSpek<HoconSource>({
    subject { HoconProvider.fromString("key = 1") as HoconSource }

    given("a HOCON source") {
        on("get underlying config") {
            it("should return corresponding config") {
                val config = subject.config
                assertThat(config.getInt("key"), equalTo(1))
            }
        }
        on("get an existed key") {
            it("should contain the key") {
                assertTrue(subject.contains("key".toPath()))
            }
            it("should contain the corresponding value") {
                assertThat((subject.getOrNull("key".toPath()) as HoconValueSource).toInt(), equalTo(1))
            }
        }
        on("get an non-existed key") {
            it("should not contain the key") {
                assertFalse(subject.contains("invalid".toPath()))
            }
            it("should not contain the corresponding value") {
                assertNull(subject.getOrNull("invalid".toPath()))
            }
        }
    }
})
