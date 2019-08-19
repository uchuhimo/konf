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

package com.uchuhimo.konf.source

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.uchuhimo.konf.Config
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

object MultipleDefaultLoadersSpec : Spek({
    on("load from multiple sources") {
        val config = Config {
            addSpec(DefaultLoadersConfig)
        }
        val item = DefaultLoadersConfig.type
        val afterLoadEnv = config.from.env()
        System.setProperty(config.nameOf(DefaultLoadersConfig.type), "system")
        val afterLoadSystemProperties = afterLoadEnv.from.systemProperties()
        val afterLoadHocon = afterLoadSystemProperties.from.hocon.string(hoconContent)
        val afterLoadJson = afterLoadHocon.from.json.string(jsonContent)
        val afterLoadProperties = afterLoadJson.from.properties.string(propertiesContent)
        val afterLoadToml = afterLoadProperties.from.toml.string(tomlContent)
        val afterLoadXml = afterLoadToml.from.xml.string(xmlContent)
        val afterLoadYaml = afterLoadXml.from.yaml.string(yamlContent)
        val afterLoadFlat = afterLoadYaml.from.map.flat(mapOf("source.test.type" to "flat"))
        val afterLoadKv = afterLoadFlat.from.map.kv(mapOf("source.test.type" to "kv"))
        val afterLoadHierarchical = afterLoadKv.from.map.hierarchical(
            mapOf("source" to
                mapOf("test" to
                    mapOf("type" to "hierarchical"))))
        it("should load the corresponding value in each layer") {
            assertThat(afterLoadEnv[item], equalTo("env"))
            assertThat(afterLoadSystemProperties[item], equalTo("system"))
            assertThat(afterLoadHocon[item], equalTo("conf"))
            assertThat(afterLoadJson[item], equalTo("json"))
            assertThat(afterLoadProperties[item], equalTo("properties"))
            assertThat(afterLoadToml[item], equalTo("toml"))
            assertThat(afterLoadXml[item], equalTo("xml"))
            assertThat(afterLoadYaml[item], equalTo("yaml"))
            assertThat(afterLoadFlat[item], equalTo("flat"))
            assertThat(afterLoadKv[item], equalTo("kv"))
            assertThat(afterLoadHierarchical[item], equalTo("hierarchical"))
        }
    }
})

const val jsonContent = """
{
  "source": {
    "test": {
      "type": "json"
    }
  }
}
"""
