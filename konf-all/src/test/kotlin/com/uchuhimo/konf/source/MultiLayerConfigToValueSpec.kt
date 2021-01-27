/*
 * Copyright 2017-2021 the original author or authors.
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

import com.fasterxml.jackson.databind.DeserializationFeature
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.toValue
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

object MultiLayerConfigToValueSpec : Spek({
    val yamlContent = """
db:
  driverClassName: org.h2.Driver
  url: 'jdbc:h2:mem:db;DB_CLOSE_DELAY=-1'
    """.trimIndent()

    val map = mapOf(
        "driverClassName" to "org.h2.Driver",
        "url" to "jdbc:h2:mem:db;DB_CLOSE_DELAY=-1"
    )
    on("load from multiple sources") {
        val config = Config {
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
            .from.yaml.string(yamlContent)
            .from.yaml.file(
                System.getenv("SERVICE_CONFIG")
                    ?: "/opt/legacy-event-service/conf/legacy-event-service.yml",
                true
            )
            .from.systemProperties()
            .from.env()
        it("should cast to value correctly") {
            val db = config.toValue<ConfigTestReport>()
            assertThat(db.db, equalTo(map))
        }
    }
})

data class ConfigTestReport(val db: Map<String, String>)
