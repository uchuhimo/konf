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
import com.natpryce.hamkrest.throws
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import com.uchuhimo.konf.tempFileOf
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.newSingleThreadContext
import kotlinx.coroutines.experimental.runBlocking
import org.eclipse.jgit.api.Git
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek
import spark.Service
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

object LoaderSpec : SubjectSpek<Loader>({
    val parentConfig = Config {
        addSpec(SourceType)
    }
    subject {
        parentConfig.from.properties
    }

    given("a loader") {
        it("should fork from parent config") {
            assertThat(subject.config, equalTo(parentConfig))
        }
        on("load from reader") {
            val config = subject.reader("type = reader".reader())
            it("should return a config which contains value from reader") {
                assertThat(config[SourceType.type], equalTo("reader"))
            }
        }
        on("load from input stream") {
            val config = subject.inputStream(
                tempFileOf("type = inputStream").inputStream())
            it("should return a config which contains value from input stream") {
                assertThat(config[SourceType.type], equalTo("inputStream"))
            }
        }
        on("load from file") {
            val config = subject.file(tempFileOf("type = file"))
            it("should return a config which contains value in file") {
                assertThat(config[SourceType.type], equalTo("file"))
            }
        }
        on("load from file path") {
            val config = subject.file(tempFileOf("type = file").toString())
            it("should return a config which contains value in file") {
                assertThat(config[SourceType.type], equalTo("file"))
            }
        }
        on("load from watched file") {
            newSingleThreadContext("context").use { context ->
                val file = tempFileOf("type = originalValue")
                val config = subject.watchFile(file, context = context)
                val originalValue = config[SourceType.type]
                file.writeText("type = newValue")
                runBlocking(context) {
                    delay(5, TimeUnit.SECONDS)
                }
                val newValue = config[SourceType.type]
                it("should return a config which contains value in file") {
                    assertThat(originalValue, equalTo("originalValue"))
                }
                it("should load new value when file has been changed") {
                    assertThat(newValue, equalTo("newValue"))
                }
            }
        }
        on("load from watched file path") {
            newSingleThreadContext("context").use { context ->
                val file = tempFileOf("type = originalValue")
                val config = subject.watchFile(file.toString(), context = context)
                val originalValue = config[SourceType.type]
                file.writeText("type = newValue")
                runBlocking(context) {
                    delay(5, TimeUnit.SECONDS)
                }
                val newValue = config[SourceType.type]
                it("should return a config which contains value in file") {
                    assertThat(originalValue, equalTo("originalValue"))
                }
                it("should load new value when file has been changed") {
                    assertThat(newValue, equalTo("newValue"))
                }
            }
        }
        on("load from string") {
            val config = subject.string("type = string")
            it("should return a config which contains value in string") {
                assertThat(config[SourceType.type], equalTo("string"))
            }
        }
        on("load from byte array") {
            val config = subject.bytes("type = bytes".toByteArray())
            it("should return a config which contains value in byte array") {
                assertThat(config[SourceType.type], equalTo("bytes"))
            }
        }
        on("load from byte array slice") {
            val config = subject.bytes("|type = slice|".toByteArray(), 1, 12)
            it("should return a config which contains value in byte array slice") {
                assertThat(config[SourceType.type], equalTo("slice"))
            }
        }
        on("load from HTTP URL") {
            val service = Service.ignite()
            service.port(0)
            service.get("/source") { _, _ -> "type = http" }
            service.awaitInitialization()
            val config = subject.url("http://localhost:${service.port()}/source")
            it("should return a config which contains value in URL") {
                assertThat(config[SourceType.type], equalTo("http"))
            }
            service.stop()
        }
        on("load from file URL") {
            val file = tempFileOf("type = fileUrl")
            val config = subject.url(file.toURI().toURL())
            it("should return a config which contains value in URL") {
                assertThat(config[SourceType.type], equalTo("fileUrl"))
            }
        }
        on("load from file URL string") {
            val url = tempFileOf("type = fileUrl").toURI().toURL().toString()
            val config = subject.url(url)
            it("should return a config which contains value in URL") {
                assertThat(config[SourceType.type], equalTo("fileUrl"))
            }
        }
        on("load from watched HTTP URL") {
            newSingleThreadContext("context").use { context ->
                var content = "type = originalValue"
                val service = Service.ignite()
                service.port(0)
                service.get("/source") { _, _ -> content }
                service.awaitInitialization()
                val url = "http://localhost:${service.port()}/source"
                val config = subject.watchUrl(url, context = context)
                val originalValue = config[SourceType.type]
                content = "type = newValue"
                runBlocking(context) {
                    delay(5, TimeUnit.SECONDS)
                }
                val newValue = config[SourceType.type]
                it("should return a config which contains value in URL") {
                    assertThat(originalValue, equalTo("originalValue"))
                }
                it("should load new value after URL content has been changed") {
                    assertThat(newValue, equalTo("newValue"))
                }
            }
        }
        on("load from watched file URL") {
            newSingleThreadContext("context").use { context ->
                val file = tempFileOf("type = originalValue")
                val config = subject.watchUrl(file.toURI().toURL(), context = context)
                val originalValue = config[SourceType.type]
                file.writeText("type = newValue")
                runBlocking(context) {
                    delay(5, TimeUnit.SECONDS)
                }
                val newValue = config[SourceType.type]
                it("should return a config which contains value in URL") {
                    assertThat(originalValue, equalTo("originalValue"))
                }
                it("should load new value after URL content has been changed") {
                    assertThat(newValue, equalTo("newValue"))
                }
            }
        }
        on("load from watched file URL string") {
            newSingleThreadContext("context").use { context ->
                val file = tempFileOf("type = originalValue")
                val url = file.toURI().toURL()
                val config = subject.watchUrl(url.toString(), context = context)
                val originalValue = config[SourceType.type]
                file.writeText("type = newValue")
                runBlocking(context) {
                    delay(5, TimeUnit.SECONDS)
                }
                val newValue = config[SourceType.type]
                it("should return a config which contains value in URL") {
                    assertThat(originalValue, equalTo("originalValue"))
                }
                it("should load new value after URL content has been changed") {
                    assertThat(newValue, equalTo("newValue"))
                }
            }
        }
        on("load from resource") {
            val config = subject.resource("source/provider.properties")
            it("should return a config which contains value in resource") {
                assertThat(config[SourceType.type], equalTo("resource"))
            }
        }
        on("load from non-existed resource") {
            it("should throw SourceNotFoundException") {
                assertThat({ subject.resource("source/no-provider.properties") },
                    throws<SourceNotFoundException>())
            }
        }
        on("load from git repository") {
            createTempDir().let { dir ->
                Git.init().apply {
                    setDirectory(dir)
                }.call().use { git ->
                    Paths.get(dir.path, "test").toFile().writeText("type = git")
                    git.add().apply {
                        addFilepattern("test")
                    }.call()
                    git.commit().apply {
                        message = "init commit"
                    }.call()
                }
                val repo = dir.toURI()
                val config = subject.git(repo.toString(), "test")
                it("should return a config which contains value in git repository") {
                    assertThat(config[SourceType.type], equalTo("git"))
                }
            }
        }
        on("load from watched git repository") {
            newSingleThreadContext("context").use { context ->
                createTempDir(prefix = "remote_git_repo", suffix = ".git").let { dir ->
                    val file = Paths.get(dir.path, "test").toFile()
                    Git.init().apply {
                        setDirectory(dir)
                    }.call().use { git ->
                        file.writeText("type = originalValue")
                        git.add().apply {
                            addFilepattern("test")
                        }.call()
                        git.commit().apply {
                            message = "init commit"
                        }.call()
                    }
                    val repo = dir.toURI()
                    val config = subject.watchGit(
                        repo.toString(), "test",
                        period = 1, unit = TimeUnit.SECONDS, context = context)
                    val originalValue = config[SourceType.type]
                    file.writeText("type = newValue")
                    Git.open(dir).use { git ->
                        git.add().apply {
                            addFilepattern("test")
                        }.call()
                        git.commit().apply {
                            message = "update value"
                        }.call()
                    }
                    runBlocking(context) {
                        delay(1, TimeUnit.SECONDS)
                    }
                    val newValue = config[SourceType.type]
                    it("should return a config which contains value in git repository") {
                        assertThat(originalValue, equalTo("originalValue"))
                    }
                    it("should load new value when content of git repository has been changed") {
                        assertThat(newValue, equalTo("newValue"))
                    }
                }
            }
        }
        on("load from watched git repository to the given directory") {
            newSingleThreadContext("context").use { context ->
                createTempDir(prefix = "remote_git_repo", suffix = ".git").let { dir ->
                    val file = Paths.get(dir.path, "test").toFile()
                    Git.init().apply {
                        setDirectory(dir)
                    }.call().use { git ->
                        file.writeText("type = originalValue")
                        git.add().apply {
                            addFilepattern("test")
                        }.call()
                        git.commit().apply {
                            message = "init commit"
                        }.call()
                    }
                    val repo = dir.toURI()
                    val config = subject.watchGit(
                        repo.toString(), "test",
                        dir = createTempDir(prefix = "local_git_repo").path,
                        period = 1, unit = TimeUnit.SECONDS, context = context)
                    val originalValue = config[SourceType.type]
                    file.writeText("type = newValue")
                    Git.open(dir).use { git ->
                        git.add().apply {
                            addFilepattern("test")
                        }.call()
                        git.commit().apply {
                            message = "update value"
                        }.call()
                    }
                    runBlocking(context) {
                        delay(1, TimeUnit.SECONDS)
                    }
                    val newValue = config[SourceType.type]
                    it("should return a config which contains value in git repository") {
                        assertThat(originalValue, equalTo("originalValue"))
                    }
                    it("should load new value when content of git repository has been changed") {
                        assertThat(newValue, equalTo("newValue"))
                    }
                }
            }
        }
    }
})

private object SourceType : ConfigSpec() {
    val type by required<String>()
}
