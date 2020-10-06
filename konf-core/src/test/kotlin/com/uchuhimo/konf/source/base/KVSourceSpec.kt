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

package com.uchuhimo.konf.source.base

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.uchuhimo.konf.source.asValue
import com.uchuhimo.konf.toPath
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

object KVSourceSpec : SubjectSpek<KVSource>({
    subject { KVSource(map = mapOf("1" to 1)) }

    given("a KV source") {
        on("get the underlying map") {
            it("should return the specified map") {
                assertThat(subject.map, equalTo(mapOf("1" to 1 as Any)))
            }
        }
        on("get an existed key") {
            it("should contain the key") {
                assertTrue("1".toPath() in subject)
            }
            it("should contain the corresponding value") {
                assertThat(subject.getOrNull("1".toPath())?.asValue<Int>(), equalTo(1))
            }
        }
        on("get an non-existed key") {
            it("should not contain the key") {
                assertFalse("2".toPath() in subject)
            }
            it("should not contain the corresponding value") {
                assertNull(subject.getOrNull("2".toPath()))
            }
        }
    }
})
