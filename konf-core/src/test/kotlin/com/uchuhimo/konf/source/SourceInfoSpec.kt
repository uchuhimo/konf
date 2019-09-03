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

package com.uchuhimo.konf.source

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek

object SourceInfoSpec : SubjectSpek<SourceInfo>({
    subject { SourceInfo("a" to "1") }

    given("a source info") {
        on("use as a map") {
            it("should behave like a map") {
                assertThat(subject.toMap(), equalTo(mapOf("a" to "1")))
            }
        }
        on("with new KV pairs") {
            it("should contain the new KV pairs") {
                assertThat(subject.with("b" to "2", "c" to "3").toMap(),
                    equalTo(mapOf("a" to "1", "b" to "2", "c" to "3")))
            }
        }
        on("with another source info") {
            it("should contain the new KV pairs in another source info") {
                assertThat(subject.with(SourceInfo("b" to "2", "c" to "3")).toMap(),
                    equalTo(mapOf("a" to "1", "b" to "2", "c" to "3")))
            }
        }
    }
})
