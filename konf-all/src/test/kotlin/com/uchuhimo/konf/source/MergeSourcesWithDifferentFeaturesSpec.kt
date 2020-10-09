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

package com.uchuhimo.konf.source

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

object MergeSourcesWithDifferentFeaturesSpec : Spek({
    on("load from merged sources") {
        val config = Config {
            addSpec(ServicingConfig)
        }.withSource(
            Source.from.hocon.string(content) + Source.from.env()
        )
        it("should contain the item") {
            assertThat(config[ServicingConfig.baseURL], equalTo("https://service/api"))
            assertThat(config[ServicingConfig.url], equalTo("https://service/api/index.html"))
        }
    }
})

object ServicingConfig : ConfigSpec("servicing") {
    val baseURL by required<String>()
    val url by required<String>()
}

val content = """
    servicing {
      baseURL = "https://service/api"
      url = "${'$'}{servicing.baseURL}/index.html"
    }
""".trimIndent()
