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
import com.natpryce.hamkrest.throws
import com.uchuhimo.konf.source.properties.PropertiesProvider
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek
import java.nio.file.Paths
import kotlin.test.assertTrue

object GitProviderSpec : SubjectSpek<Provider>({
    subject { PropertiesProvider }

    given("a provider") {
        on("create source from git repository") {
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
                val source = subject.fromGit(repo.toString(), "test")
                it("should create from the specified git repository") {
                    assertThat(source.info["repo"], equalTo(repo.toString()))
                    assertThat(source.info["file"], equalTo("test"))
                    assertThat(source.info["branch"], equalTo(Constants.HEAD))
                }
                it("should return a source which contains value in git repository") {
                    assertThat(source["type"].asValue<String>(), equalTo("git"))
                }
            }
        }
        on("create source from invalid git repository") {
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
                it("should throw InvalidRemoteRepoException") {
                    assertThat({ subject.fromGit(createTempDir().path, "test", dir = dir.path) },
                        throws<InvalidRemoteRepoException>())
                }
                it("should return an empty source if optional") {
                    assertTrue {
                        subject.fromGit(createTempDir().path, "test", dir = dir.path, optional = true).tree.children.isEmpty()
                    }
                }
            }
        }
    }
})
