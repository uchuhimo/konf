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
import com.natpryce.hamkrest.sameInstance
import com.natpryce.hamkrest.throws
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import com.uchuhimo.konf.source.properties.PropertiesProvider
import com.uchuhimo.konf.tempFileOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek
import org.jetbrains.spek.subject.itBehavesLike
import spark.Service
import java.net.URL
import java.util.UUID
import java.util.concurrent.TimeUnit

object DefaultLoadersSpec : SubjectSpek<DefaultLoaders>({
    subject {
        Config {
            addSpec(DefaultLoadersConfig)
        }.from
    }

    val item = DefaultLoadersConfig.type

    given("a loader") {
        on("load from environment-like map") {
            val config = subject.envMap(mapOf("SOURCE_TEST_TYPE" to "env"))
            it("should return a config which contains value from environment-like map") {
                assertThat(config[item], equalTo("env"))
            }
        }
        on("load from system environment") {
            val config = subject.env()
            it("should return a config which contains value from system environment") {
                assertThat(config[item], equalTo("env"))
            }
        }
        on("load from system properties") {
            System.setProperty(DefaultLoadersConfig.qualify(DefaultLoadersConfig.type), "system")
            val config = subject.systemProperties()
            it("should return a config which contains value from system properties") {
                assertThat(config[item], equalTo("system"))
            }
        }
        on("dispatch loader based on extension") {
            it("should throw UnsupportedExtensionException when the extension is unsupported") {
                assertThat({ subject.dispatchExtension("txt") }, throws<UnsupportedExtensionException>())
            }
            it("should return the corresponding loader when the extension is registered") {
                val extension = UUID.randomUUID().toString()
                Provider.registerExtension(extension, PropertiesProvider)
                subject.dispatchExtension(extension)
                Provider.unregisterExtension(extension)
            }
        }
        on("load from provider") {
            val config = subject.source(PropertiesProvider).file(tempFileOf(propertiesContent, suffix = ".properties"))
            it("should load with the provider") {
                assertThat(config[item], equalTo("properties"))
            }
            it("should build a new layer on the parent config") {
                assertThat(config.parent!!, sameInstance(subject.config))
            }
        }
        on("load from URL") {
            val service = Service.ignite()
            service.port(0)
            service.get("/source.properties") { _, _ -> propertiesContent }
            service.awaitInitialization()
            val config = subject.url(URL("http://localhost:${service.port()}/source.properties"))
            it("should load as auto-detected URL format") {
                assertThat(config[item], equalTo("properties"))
            }
            service.stop()
        }
        on("load from URL string") {
            val service = Service.ignite()
            service.port(0)
            service.get("/source.properties") { _, _ -> propertiesContent }
            service.awaitInitialization()
            val config = subject.url("http://localhost:${service.port()}/source.properties")
            it("should load as auto-detected URL format") {
                assertThat(config[item], equalTo("properties"))
            }
            service.stop()
        }
        on("load from file") {
            val config = subject.file(tempFileOf(propertiesContent, suffix = ".properties"))
            it("should load as auto-detected file format") {
                assertThat(config[item], equalTo("properties"))
            }
        }
        on("load from file path") {
            val file = tempFileOf(propertiesContent, suffix = ".properties")
            val config = subject.file(file.path)
            it("should load as auto-detected file format") {
                assertThat(config[item], equalTo("properties"))
            }
        }
        on("load from watched file") {
            val file = tempFileOf(propertiesContent, suffix = ".properties")
            val config = subject.watchFile(file, 1, unit = TimeUnit.SECONDS, context = Dispatchers.Sequential)
            val originalValue = config[item]
            file.writeText(propertiesContent.replace("properties", "newValue"))
            runBlocking(Dispatchers.Sequential) {
                delay(TimeUnit.SECONDS.toMillis(1))
            }
            val newValue = config[item]
            it("should load as auto-detected file format") {
                assertThat(originalValue, equalTo("properties"))
            }
            it("should load new value when file has been changed") {
                assertThat(newValue, equalTo("newValue"))
            }
        }
        on("load from watched file with default delay time") {
            val file = tempFileOf(propertiesContent, suffix = ".properties")
            val config = subject.watchFile(file, context = Dispatchers.Sequential)
            val originalValue = config[item]
            file.writeText(propertiesContent.replace("properties", "newValue"))
            runBlocking(Dispatchers.Sequential) {
                delay(TimeUnit.SECONDS.toMillis(5))
            }
            val newValue = config[item]
            it("should load as auto-detected file format") {
                assertThat(originalValue, equalTo("properties"))
            }
            it("should load new value when file has been changed") {
                assertThat(newValue, equalTo("newValue"))
            }
        }
        on("load from watched file path") {
            val file = tempFileOf(propertiesContent, suffix = ".properties")
            val config = subject.watchFile(file.path, 1, unit = TimeUnit.SECONDS, context = Dispatchers.Sequential)
            val originalValue = config[item]
            file.writeText(propertiesContent.replace("properties", "newValue"))
            runBlocking(Dispatchers.Sequential) {
                delay(TimeUnit.SECONDS.toMillis(1))
            }
            val newValue = config[item]
            it("should load as auto-detected file format") {
                assertThat(originalValue, equalTo("properties"))
            }
            it("should load new value when file has been changed") {
                assertThat(newValue, equalTo("newValue"))
            }
        }
        on("load from watched file path with default delay time") {
            val file = tempFileOf(propertiesContent, suffix = ".properties")
            val config = subject.watchFile(file.path, context = Dispatchers.Sequential)
            val originalValue = config[item]
            file.writeText(propertiesContent.replace("properties", "newValue"))
            runBlocking(Dispatchers.Sequential) {
                delay(TimeUnit.SECONDS.toMillis(5))
            }
            val newValue = config[item]
            it("should load as auto-detected file format") {
                assertThat(originalValue, equalTo("properties"))
            }
            it("should load new value when file has been changed") {
                assertThat(newValue, equalTo("newValue"))
            }
        }
        on("load from watched URL") {
            var content = propertiesContent
            val service = Service.ignite()
            service.port(0)
            service.get("/source.properties") { _, _ -> content }
            service.awaitInitialization()
            val url = "http://localhost:${service.port()}/source.properties"
            val config = subject.watchUrl(URL(url), period = 1, unit = TimeUnit.SECONDS, context = Dispatchers.Sequential)
            val originalValue = config[item]
            content = propertiesContent.replace("properties", "newValue")
            runBlocking(Dispatchers.Sequential) {
                delay(TimeUnit.SECONDS.toMillis(1))
            }
            val newValue = config[item]
            it("should load as auto-detected URL format") {
                assertThat(originalValue, equalTo("properties"))
            }
            it("should load new value after URL content has been changed") {
                assertThat(newValue, equalTo("newValue"))
            }
        }
        on("load from watched URL with default delay time") {
            var content = propertiesContent
            val service = Service.ignite()
            service.port(0)
            service.get("/source.properties") { _, _ -> content }
            service.awaitInitialization()
            val url = "http://localhost:${service.port()}/source.properties"
            val config = subject.watchUrl(URL(url), context = Dispatchers.Sequential)
            val originalValue = config[item]
            content = propertiesContent.replace("properties", "newValue")
            runBlocking(Dispatchers.Sequential) {
                delay(TimeUnit.SECONDS.toMillis(5))
            }
            val newValue = config[item]
            it("should load as auto-detected URL format") {
                assertThat(originalValue, equalTo("properties"))
            }
            it("should load new value after URL content has been changed") {
                assertThat(newValue, equalTo("newValue"))
            }
        }
        on("load from watched URL string") {
            var content = propertiesContent
            val service = Service.ignite()
            service.port(0)
            service.get("/source.properties") { _, _ -> content }
            service.awaitInitialization()
            val url = "http://localhost:${service.port()}/source.properties"
            val config = subject.watchUrl(url, period = 1, unit = TimeUnit.SECONDS, context = Dispatchers.Sequential)
            val originalValue = config[item]
            content = propertiesContent.replace("properties", "newValue")
            runBlocking(Dispatchers.Sequential) {
                delay(TimeUnit.SECONDS.toMillis(1))
            }
            val newValue = config[item]
            it("should load as auto-detected URL format") {
                assertThat(originalValue, equalTo("properties"))
            }
            it("should load new value after URL content has been changed") {
                assertThat(newValue, equalTo("newValue"))
            }
        }
        on("load from watched URL string with default delay time") {
            var content = propertiesContent
            val service = Service.ignite()
            service.port(0)
            service.get("/source.properties") { _, _ -> content }
            service.awaitInitialization()
            val url = "http://localhost:${service.port()}/source.properties"
            val config = subject.watchUrl(url, context = Dispatchers.Sequential)
            val originalValue = config[item]
            content = propertiesContent.replace("properties", "newValue")
            runBlocking(Dispatchers.Sequential) {
                delay(TimeUnit.SECONDS.toMillis(5))
            }
            val newValue = config[item]
            it("should load as auto-detected URL format") {
                assertThat(originalValue, equalTo("properties"))
            }
            it("should load new value after URL content has been changed") {
                assertThat(newValue, equalTo("newValue"))
            }
        }
        on("load from map") {
            it("should use the same config") {
                assertThat(subject.config, sameInstance(subject.map.config))
            }
        }
    }
})

object DefaultLoadersWithFlattenEnvSpec : Spek({
    given("a loader") {
        on("load as flatten format from system environment") {
            val config = Config {
                addSpec(FlattenDefaultLoadersConfig)
            }.from.env(nested = false)
            it("should return a config which contains value from system environment") {
                assertThat(config[FlattenDefaultLoadersConfig.SOURCE_TEST_TYPE], equalTo("env"))
            }
        }
    }
})

object MappedDefaultLoadersSpec : SubjectSpek<DefaultLoaders>({
    subject {
        Config {
            addSpec(DefaultLoadersConfig["source"])
        }.from.mapped { it["source"] }
    }

    itBehavesLike(DefaultLoadersSpec)
})

object PrefixedDefaultLoadersSpec : SubjectSpek<DefaultLoaders>({
    subject {
        Config {
            addSpec(DefaultLoadersConfig.withPrefix("prefix"))
        }.from.prefixed("prefix")
    }

    itBehavesLike(DefaultLoadersSpec)
})

object ScopedDefaultLoadersSpec : SubjectSpek<DefaultLoaders>({
    subject {
        Config {
            addSpec(DefaultLoadersConfig["source"])
        }.from.scoped("source")
    }

    itBehavesLike(DefaultLoadersSpec)
})

object FlattenDefaultLoadersConfig : ConfigSpec("") {
    val SOURCE_TEST_TYPE by required<String>()
}
