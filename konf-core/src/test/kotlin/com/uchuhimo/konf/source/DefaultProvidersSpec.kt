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
import com.natpryce.hamkrest.sameInstance
import com.natpryce.hamkrest.throws
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.properties.PropertiesProvider
import com.uchuhimo.konf.tempFileOf
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek
import spark.Service
import java.net.URL
import java.util.UUID

object DefaultProvidersSpec : SubjectSpek<DefaultProviders>({
    subject { Source.from }

    val item = DefaultLoadersConfig.type

    given("a provider") {
        on("provide source from system environment") {
            val config = subject.env().toConfig()
            it("should return a source which contains value from system environment") {
                assertThat(config[item], equalTo("env"))
            }
        }
        on("provide flatten source from system environment") {
            val config = subject.env(nested = false).toFlattenConfig()
            it("should return a source which contains value from system environment") {
                assertThat(config[FlattenDefaultLoadersConfig.SOURCE_TEST_TYPE], equalTo("env"))
            }
        }
        on("provide source from system properties") {
            System.setProperty(DefaultLoadersConfig.qualify(DefaultLoadersConfig.type), "system")
            val config = subject.systemProperties().toConfig()
            it("should return a source which contains value from system properties") {
                assertThat(config[item], equalTo("system"))
            }
        }
        on("dispatch provider based on extension") {
            it("should throw UnsupportedExtensionException when the extension is unsupported") {
                assertThat({ subject.dispatchExtension("txt") }, throws<UnsupportedExtensionException>())
            }
            it("should return the corresponding provider when the extension is registered") {
                val extension = UUID.randomUUID().toString()
                Provider.registerExtension(extension, PropertiesProvider)
                assertThat(subject.dispatchExtension(extension), sameInstance(PropertiesProvider as Provider))
                Provider.unregisterExtension(extension)
            }
        }
        on("provide source from URL") {
            val service = Service.ignite()
            service.port(0)
            service.get("/source.properties") { _, _ -> propertiesContent }
            service.awaitInitialization()
            val config = subject.url(URL("http://localhost:${service.port()}/source.properties")).toConfig()
            it("should provide as auto-detected URL format") {
                assertThat(config[item], equalTo("properties"))
            }
            service.stop()
        }
        on("provide source from URL string") {
            val service = Service.ignite()
            service.port(0)
            service.get("/source.properties") { _, _ -> propertiesContent }
            service.awaitInitialization()
            val config = subject.url("http://localhost:${service.port()}/source.properties").toConfig()
            it("should provide as auto-detected URL format") {
                assertThat(config[item], equalTo("properties"))
            }
            service.stop()
        }
        on("provide source from file") {
            val config = subject.file(tempFileOf(propertiesContent, suffix = ".properties")).toConfig()
            it("should provide as auto-detected file format") {
                assertThat(config[item], equalTo("properties"))
            }
        }
        on("provide source from file path") {
            val file = tempFileOf(propertiesContent, suffix = ".properties")
            val config = subject.file(file.path).toConfig()
            it("should provide as auto-detected file format") {
                assertThat(config[item], equalTo("properties"))
            }
        }
    }
})

fun Source.toFlattenConfig(): Config = Config {
    addSpec(FlattenDefaultLoadersConfig)
}.withSource(this)
