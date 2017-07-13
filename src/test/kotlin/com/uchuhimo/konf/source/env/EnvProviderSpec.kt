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

package com.uchuhimo.konf.source.env

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek

object EnvProviderSpec : SubjectSpek<EnvProvider>({
    subject { EnvProvider }

    given("a source provider") {
        on("create source from system environment") {
            val source = subject.fromEnv()
            it("should have correct type") {
                assertThat(source.info["type"], equalTo("system-environment"))
            }
            it("should return a source which contains value from system environment") {
                assertThat(source.get(listOf("source", "test", "type")).toText(), equalTo("env"))
            }
        }
    }
})
