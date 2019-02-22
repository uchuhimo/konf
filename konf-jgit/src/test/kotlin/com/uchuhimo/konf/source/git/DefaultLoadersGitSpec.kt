package com.uchuhimo.konf.source.git

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import com.uchuhimo.konf.source.DefaultLoaders
import com.uchuhimo.konf.source.Sequential
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

object DefaultLoadersGitSpec : SubjectSpek<DefaultLoaders>({
    subject {
        Config {
            addSpec(DefaultLoadersConfig)
        }.from
    }

    val item = DefaultLoadersConfig.type

    given("a loader") {
        on("load from git repository") {
            createTempDir().let { dir ->
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
        on("load from watched git repository") {
            createTempDir(prefix = "remote_git_repo", suffix = ".git").let { dir ->
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
                val config = subject.watchGit(
                    repo.toString(), "source.properties",
                    period = 1, unit = TimeUnit.SECONDS, context = Dispatchers.Sequential)
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
})

private object DefaultLoadersConfig : ConfigSpec("source.test") {
    val type by required<String>()
}

private const val propertiesContent = "source.test.type = properties"