package com.uchuhimo.konf.source.git

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.Loader
import com.uchuhimo.konf.source.Sequential
import com.uchuhimo.konf.source.SourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.api.Git
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

object LoaderGitSpek : SubjectSpek<Loader>({
    val parentConfig = Config {
        addSpec(SourceType)
    }
    subject {
        parentConfig.from.properties
    }

    given("a loader") {
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
                    period = 1, unit = TimeUnit.SECONDS, context = Dispatchers.Sequential)
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
                runBlocking(Dispatchers.Sequential) {
                    delay(TimeUnit.SECONDS.toMillis(1))
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
        on("load from watched git repository to the given directory") {
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
                    period = 1, unit = TimeUnit.SECONDS, context = Dispatchers.Sequential)
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
                runBlocking(Dispatchers.Sequential) {
                    delay(TimeUnit.SECONDS.toMillis(1))
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
})
