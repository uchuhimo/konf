package com.uchuhimo.konf.source

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import com.uchuhimo.konf.tempFileOf
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek

object DefaultLoadersSpec : SubjectSpek<DefaultLoaders>({
    subject {
        Config {
            addSpec(DefaultLoadersConfig)
        }.loadFrom
    }

    val item = DefaultLoadersConfig.type

    given("a loader") {
        on("load from system environment") {
            val config = subject.env()
            it("should return a config which contains value from system environment") {
                assertThat(config[item], equalTo("env"))
            }
        }
        on("load from system properties") {
            System.setProperty(DefaultLoadersConfig.qualify("type"), "system")
            val config = subject.systemProperties()
            it("should return a config which contains value from system properties") {
                assertThat(config[item], equalTo("system"))
            }
        }
        group("load from file") {
            on("load from file with .conf extension") {
                val config = subject.file(
                        tempFileOf("source.test.type = hocon", suffix = ".conf"))
                it("should load as HOCON format") {
                    assertThat(config[item], equalTo("hocon"))
                }
            }
            on("load from file with .json extension") {
                val config = subject.file(tempFileOf(jsonContent, suffix = ".json"))
                it("should load as JSON format") {
                    assertThat(config[item], equalTo("json"))
                }
            }
            on("load from file with .properties extension") {
                val config = subject.file(
                        tempFileOf("source.test.type = properties", suffix = ".properties"))
                it("should load as properties file format") {
                    assertThat(config[item], equalTo("properties"))
                }
            }
            on("load from file with .toml extension") {
                val config = subject.file(tempFileOf(tomlContent, suffix = ".toml"))
                it("should load as TOML format") {
                    assertThat(config[item], equalTo("toml"))
                }
            }
            on("load from file with .xml extension") {
                val config = subject.file(tempFileOf(xmlContent, suffix = ".xml"))
                it("should load as XML format") {
                    assertThat(config[item], equalTo("xml"))
                }
            }
            on("load from file with .yaml extension") {
                val config = subject.file(tempFileOf(yamlContent, suffix = ".yaml"))
                it("should load as YAML format") {
                    assertThat(config[item], equalTo("yaml"))
                }
            }
            on("load from file with .yml extension") {
                val config = subject.file(tempFileOf(ymlContent, suffix = ".yml"))
                it("should load as YAML format") {
                    assertThat(config[item], equalTo("yml"))
                }
            }
            on("load from file with unsupported extension") {
                it("should throw UnsupportedExtensionException") {
                    assertThat({
                        subject.file(tempFileOf("source.test.type = txt", suffix = ".txt"))
                    }, throws<UnsupportedExtensionException>())
                }
            }
        }
        on("load from multiple sources") {
            val afterLoadEnv = subject.env()
            System.setProperty(DefaultLoadersConfig.qualify("type"), "system")
            val afterLoadSystemProperties = afterLoadEnv.loadFrom
                    .systemProperties()
            val afterLoadHocon = afterLoadSystemProperties.loadFrom
                    .hocon.string("source.test.type = hocon")
            val afterLoadJson = afterLoadHocon.loadFrom
                    .json.string(jsonContent)
            val afterLoadProperties = afterLoadJson.loadFrom
                    .properties.string("source.test.type = properties")
            val afterLoadToml = afterLoadProperties.loadFrom
                    .toml.string(tomlContent)
            val afterLoadXml = afterLoadToml.loadFrom.
                    xml.string(xmlContent)
            val afterLoadYaml = afterLoadXml.loadFrom
                    .yaml.string(yamlContent)
            val afterLoadFlat = afterLoadYaml.loadFrom
                    .map.flat(mapOf("source.test.type" to "flat"))
            val afterLoadKv = afterLoadFlat.loadFrom.map
                    .kv(mapOf("source.test.type" to "kv"))
            val afterLoadHierarchical = afterLoadKv.loadFrom.map
                    .hierarchical(
                            mapOf("source" to
                                    mapOf("test" to
                                            mapOf("type" to "hierarchical"))))
            it("should load the corresponding value in each layer") {
                assertThat(afterLoadEnv[item], equalTo("env"))
                assertThat(afterLoadSystemProperties[item], equalTo("system"))
                assertThat(afterLoadHocon[item], equalTo("hocon"))
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
    }
})

private object DefaultLoadersConfig : ConfigSpec("source.test") {
    val type = required<String>("type")
}

private val jsonContent = """
{
  "source": {
    "test": {
      "type": "json"
    }
  }
}
"""

private val tomlContent = """
[source.test]
type = "toml"
"""

val xmlContent = """
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <property>
        <name>source.test.type</name>
        <value>xml</value>
    </property>
</configuration>
""".trim()

val yamlContent = """
source:
    test:
        type: yaml
"""

val ymlContent = """
source:
    test:
        type: yml
"""
