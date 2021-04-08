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

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.tempDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

object DefaultGitLoaderSpec : SubjectSpek<DefaultLoaders>({
    subject {
        Config {
            addSpec(DefaultLoadersConfig)
        }.from
    }

    val item = DefaultLoadersConfig.type

    given("a loader") {
        on("load from git repository") {
            tempDirectory().let { dir ->
                Git.init().apply {
                    setDirectory(dir)
                }.call().use { git ->
                    Paths.get(dir.path, "source.properties").toFile().writeText(propertiesContent)
                    git.add().apply {
                        addFilepattern("source.properties")
                    }.call()
                    git.commit().apply {
                        message = "init commit"
                    }.call()
                }
                val repo = dir.toURI()
                val config = subject.git(repo.toString(), "source.properties")
                it("should load as auto-detected file format") {
                    assertThat(config[item], equalTo("properties"))
                }
            }
        }
        mapOf(
            "load from watched git repository" to { loader: DefaultLoaders, repo: String ->
                loader.watchGit(
                    repo,
                    "source.properties",
                    period = 1,
                    unit = TimeUnit.SECONDS,
                    context = Dispatchers.Sequential
                )
            },
            "load from watched git repository to the given directory" to { loader: DefaultLoaders, repo: String ->
                loader.watchGit(
                    repo,
                    "source.properties",
                    dir = tempDirectory(prefix = "local_git_repo").path,
                    branch = Constants.HEAD,
                    unit = TimeUnit.SECONDS,
                    context = Dispatchers.Sequential,
                    optional = false
                )
            }
        ).forEach { (description, func) ->
            on(description) {
                tempDirectory(prefix = "remote_git_repo", suffix = ".git").let { dir ->
                    val file = Paths.get(dir.path, "source.properties").toFile()
                    Git.init().apply {
                        setDirectory(dir)
                    }.call().use { git ->
                        file.writeText(propertiesContent)
                        git.add().apply {
                            addFilepattern("source.properties")
                        }.call()
                        git.commit().apply {
                            message = "init commit"
                        }.call()
                    }
                    val repo = dir.toURI()
                    val config = func(subject, repo.toString())
                    val originalValue = config[item]
                    file.writeText(propertiesContent.replace("properties", "newValue"))
                    Git.open(dir).use { git ->
                        git.add().apply {
                            addFilepattern("source.properties")
                        }.call()
                        git.commit().apply {
                            message = "update value"
                        }.call()
                    }
                    runBlocking(Dispatchers.Sequential) {
                        delay(TimeUnit.SECONDS.toMillis(1))
                    }
                    val newValue = config[item]
                    it("should load as auto-detected file format") {
                        assertThat(originalValue, equalTo("properties"))
                    }
                    it("should load new value after file content in git repository has been changed") {
                        assertThat(newValue, equalTo("newValue"))
                    }
                }
            }
        }
        on("load from watched git repository with listener") {
            tempDirectory(prefix = "remote_git_repo", suffix = ".git").let { dir ->
                val file = Paths.get(dir.path, "source.properties").toFile()
                Git.init().apply {
                    setDirectory(dir)
                }.call().use { git ->
                    file.writeText(propertiesContent)
                    git.add().apply {
                        addFilepattern("source.properties")
                    }.call()
                    git.commit().apply {
                        message = "init commit"
                    }.call()
                }
                val repo = dir.toURI()
                var newValue = ""
                val config = subject.watchGit(
                    repo.toString(),
                    "source.properties",
                    period = 1,
                    unit = TimeUnit.SECONDS,
                    context = Dispatchers.Sequential
                ) { config, _ ->
                    newValue = config[item]
                }
                val originalValue = config[item]
                file.writeText(propertiesContent.replace("properties", "newValue"))
                Git.open(dir).use { git ->
                    git.add().apply {
                        addFilepattern("source.properties")
                    }.call()
                    git.commit().apply {
                        message = "update value"
                    }.call()
                }
                runBlocking(Dispatchers.Sequential) {
                    delay(TimeUnit.SECONDS.toMillis(1))
                }
                it("should load as auto-detected file format") {
                    assertThat(originalValue, equalTo("properties"))
                }
                it("should load new value after file content in git repository has been changed") {
                    assertThat(newValue, equalTo("newValue"))
                }
            }
        }
    }
})
