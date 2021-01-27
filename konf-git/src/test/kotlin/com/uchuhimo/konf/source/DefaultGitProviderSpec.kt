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
import com.uchuhimo.konf.tempDirectory
import org.eclipse.jgit.api.Git
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek
import java.nio.file.Paths

object DefaultGitProviderSpec : SubjectSpek<DefaultProviders>({
    subject { Source.from }

    val item = DefaultLoadersConfig.type

    given("a provider") {
        on("provider source from git repository") {
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
                val config = subject.git(repo.toString(), "source.properties").toConfig()
                it("should provide as auto-detected file format") {
                    assertThat(config[item], equalTo("properties"))
                }
            }
        }
    }
})
