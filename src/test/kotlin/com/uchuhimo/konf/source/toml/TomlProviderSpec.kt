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

package com.uchuhimo.konf.source.toml

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.uchuhimo.konf.tempFileOf
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek

object TomlProviderSpec : SubjectSpek<TomlProvider>({
    subject { TomlProvider }

    given("a TOML provider") {
        on("create source from reader") {
            val source = subject.fromReader("type = \"reader\"".reader())
            it("should have correct type") {
                assertThat(source.info["type"], equalTo("TOML"))
            }
            it("should return a source which contains value from reader") {
                assertThat(source["type"].toText(), equalTo("reader"))
            }
        }
        on("create source from input stream") {
            val source = subject.fromInputStream(
                    tempFileOf("type = \"inputStream\"").inputStream())
            it("should have correct type") {
                assertThat(source.info["type"], equalTo("TOML"))
            }
            it("should return a source which contains value from input stream") {
                assertThat(source["type"].toText(), equalTo("inputStream"))
            }
        }
    }
})
