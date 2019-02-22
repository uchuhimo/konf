/*
 * Copyright 2017-2018 the original author or authors.
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
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.throws
import com.uchuhimo.konf.source.UnknownPathsException
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

object FailOnUnknownPathSpec : Spek({
    val source = """
        |{
        |  "level1": {
        |    "level2": {
        |      "valid": "value1",
        |      "invalid": "value2"
        |    }
        |  }
        |}
    """.trimMargin()

    given("a config") {
        on("the feature is disabled") {
            val config = Config {
                addSpec(Valid)
            }
            it("should ignore unknown paths") {
                val conf = config.from.json.string(source)
                assertThat(conf[Valid.valid], equalTo("value1"))
            }
        }
        on("the feature is enabled on config") {
            val config = Config {
                addSpec(Valid)
            }.enable(Feature.FAIL_ON_UNKNOWN_PATH)
            it("should throws UnknownPathsException and reports the unknown paths") {
                assertThat({ config.from.json.string(source) }, throws(has(
                    UnknownPathsException::paths,
                    equalTo(listOf("level1.level2.invalid")))))
            }
        }
        on("the feature is enabled on source") {
            val config = Config {
                addSpec(Valid)
            }
            it("should throws UnknownPathsException and reports the unknown paths") {
                assertThat({
                    config.from.enabled(Feature.FAIL_ON_UNKNOWN_PATH).json.string(source)
                }, throws(has(
                    UnknownPathsException::paths,
                    equalTo(listOf("level1.level2.invalid")))))
            }
        }
    }
})

private object Valid : ConfigSpec("level1.level2") {
    val valid by required<String>()
}
