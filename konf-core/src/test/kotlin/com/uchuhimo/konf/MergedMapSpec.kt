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

package com.uchuhimo.konf

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

object MergedMapSpec : SubjectSpek<MergedMap<String, Int>>({
    subject {
        val facadeMap = mutableMapOf("a" to 1, "b" to 2)
        val fallbackMap = mutableMapOf("b" to 3, "c" to 4)
        MergedMap(fallback = fallbackMap, facade = facadeMap)
    }

    given("a merged map") {
        val mergedMap = mapOf("a" to 1, "b" to 2, "c" to 4)
        on("get size") {
            it("should return the merged size") {
                assertThat(subject.size, equalTo(3))
            }
        }
        on("query whether it contains a key") {
            it("should query in both maps") {
                assertTrue { "a" in subject }
                assertTrue { "c" in subject }
                assertFalse { "d" in subject }
            }
        }
        on("query whether it contains a value") {
            it("should query in both maps") {
                assertTrue { subject.containsValue(1) }
                assertTrue { subject.containsValue(4) }
                assertFalse { subject.containsValue(5) }
            }
        }
        on("get a value") {
            it("should query in both maps") {
                assertThat(subject["a"], equalTo(1))
                assertThat(subject["b"], equalTo(2))
                assertThat(subject["c"], equalTo(4))
                assertNull(subject["d"])
            }
        }
        on("query whether it is empty") {
            it("should query in both maps") {
                assertFalse { subject.isEmpty() }
                assertFalse { MergedMap(mutableMapOf("a" to 1), mutableMapOf()).isEmpty() }
                assertFalse { MergedMap(mutableMapOf(), mutableMapOf("a" to 1)).isEmpty() }
                assertTrue { MergedMap<String, Int>(mutableMapOf(), mutableMapOf()).isEmpty() }
            }
        }
        on("get entries") {
            it("should return entries in both maps") {
                assertThat(subject.entries, equalTo(mergedMap.entries))
            }
        }
        on("get keys") {
            it("should return keys in both maps") {
                assertThat(subject.keys, equalTo(mergedMap.keys))
            }
        }
        on("get values") {
            it("should return values in both maps") {
                assertThat(subject.values.toList(), equalTo(mergedMap.values.toList()))
            }
        }
        on("clear") {
            subject.clear()
            it("should clear both maps") {
                assertTrue { subject.isEmpty() }
                assertTrue { subject.facade.isEmpty() }
                assertTrue { subject.fallback.isEmpty() }
            }
        }
        on("put new KV pair") {
            subject["d"] = 5
            it("should put it to the facade map") {
                assertThat(subject["d"], equalTo(5))
                assertThat(subject.facade["d"], equalTo(5))
                assertNull(subject.fallback["d"])
            }
        }
        on("put new KV pairs") {
            subject.putAll(mapOf("d" to 5, "e" to 6))
            it("should put them to the facade map") {
                assertThat(subject["d"], equalTo(5))
                assertThat(subject["e"], equalTo(6))
                assertThat(subject.facade["d"], equalTo(5))
                assertThat(subject.facade["e"], equalTo(6))
                assertNull(subject.fallback["d"])
                assertNull(subject.fallback["e"])
            }
        }
        on("remove key") {
            it("should remove the key from facade map if it contains the key") {
                subject.remove("a")
                assertFalse { "a" in subject }
                assertFalse { "a" in subject.facade }
            }
            it("should remove the key from fallback map if it contains the key") {
                subject.remove("c")
                assertFalse { "c" in subject }
                assertFalse { "c" in subject.fallback }
            }
            it("should remove the key from both maps if both of them contain the key") {
                subject.remove("b")
                assertFalse { "b" in subject }
                assertFalse { "b" in subject.facade }
                assertFalse { "b" in subject.fallback }
            }
        }
    }
})
