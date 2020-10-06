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

package com.uchuhimo.konf.source.env

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek
import org.jetbrains.spek.subject.itBehavesLike
import kotlin.test.assertTrue

object EnvProviderSpec : SubjectSpek<EnvProvider>({
    subject { EnvProvider }

    given("a source provider") {
        on("create source from system environment") {
            val source = subject.env()
            it("should have correct type") {
                assertThat(source.info["type"], equalTo("system-environment"))
            }
            it("should return a source which contains value from system environment") {
                val config = Config { addSpec(SourceSpec) }.withSource(source)
                assertThat(config[SourceSpec.Test.type], equalTo("env"))
                assertTrue { config[SourceSpec.camelCase] }
            }
            it("should return a case-insensitive source") {
                val config = Config().withSource(source).apply { addSpec(SourceSpec) }
                assertThat(config[SourceSpec.Test.type], equalTo("env"))
                assertTrue { config[SourceSpec.camelCase] }
            }
        }
        on("create flatten source from system environment") {
            val source = subject.env(nested = false)
            it("should return a source which contains value from system environment") {
                val config = Config { addSpec(FlattenSourceSpec) }.withSource(source)
                assertThat(config[FlattenSourceSpec.SOURCE_TEST_TYPE], equalTo("env"))
                assertTrue { config[FlattenSourceSpec.SOURCE_CAMELCASE] }
            }
        }
        on("create source from system environment (deprecated)") {
            val source = subject.fromEnv()
            it("should have correct type") {
                assertThat(source.info["type"], equalTo("system-environment"))
            }
            it("should return a source which contains value from system environment") {
                val config = Config { addSpec(SourceSpec) }.withSource(source)
                assertThat(config[SourceSpec.Test.type], equalTo("env"))
                assertTrue { config[SourceSpec.camelCase] }
            }
            it("should return a case-insensitive source") {
                val config = Config().withSource(source).apply { addSpec(SourceSpec) }
                assertThat(config[SourceSpec.Test.type], equalTo("env"))
                assertTrue { config[SourceSpec.camelCase] }
            }
        }
    }
})

object EnvProviderInJavaSpec : SubjectSpek<EnvProvider>({
    subject { EnvProvider.get() }

    itBehavesLike(EnvProviderSpec)
})

object SourceSpec : ConfigSpec() {
    object Test : ConfigSpec() {
        val type by required<String>()
    }

    val camelCase by required<Boolean>()
}

object FlattenSourceSpec : ConfigSpec("") {
    val SOURCE_CAMELCASE by required<Boolean>()
    val SOURCE_TEST_TYPE by required<String>()
}
