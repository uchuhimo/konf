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

package com.uchuhimo.konf.source.yaml

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.Feature
import com.uchuhimo.konf.source.ConfigForLoad
import com.uchuhimo.konf.source.SourceLoadSpec
import com.uchuhimo.konf.source.yaml
import com.uchuhimo.konf.toValue
import kotlin.test.assertTrue
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek
import org.jetbrains.spek.subject.itBehavesLike

object YamlSourceLoadSpec : SubjectSpek<Config>({

    subject {
        Config {
            addSpec(ConfigForLoad)
            enable(Feature.FAIL_ON_UNKNOWN_PATH)
        }.from.yaml.resource("source/source.yaml")
    }

    itBehavesLike(SourceLoadSpec)

    given("a config") {
        on("load a YAML with a numeric key") {
            val config = Config().from.yaml.string("""
                tree:
                  1:
                    myVal: true
            """.trimIndent())
            it("should treat it as a string key") {
                assertTrue { config.at("tree.1.myVal").toValue() }
            }
        }
    }
})

object YamlSourceReloadSpec : SubjectSpek<Config>({

    subject {
        val config = Config {
            addSpec(ConfigForLoad)
        }.from.yaml.resource("source/source.yaml")
        val yaml = config.toYaml.toText()
        Config {
            addSpec(ConfigForLoad)
        }.from.yaml.string(yaml)
    }

    itBehavesLike(SourceLoadSpec)
})
