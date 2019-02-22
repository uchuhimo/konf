package com.uchuhimo.konf.source.git

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import com.uchuhimo.konf.source.InvalidRemoteRepoException
import com.uchuhimo.konf.source.Provider
import com.uchuhimo.konf.source.properties.PropertiesProvider
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek
import java.nio.file.Paths

object ProviderGitSpek : SubjectSpek<Provider>({
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
                    assertThat(source.context["repo"], equalTo(repo.toString()))
                    assertThat(source.context["file"], equalTo("test"))
                    assertThat(source.context["branch"], equalTo(Constants.HEAD))
                }
                it("should return a source which contains value in git repository") {
                    assertThat(source["type"].toText(), equalTo("git"))
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
            }
        }
    }
})
