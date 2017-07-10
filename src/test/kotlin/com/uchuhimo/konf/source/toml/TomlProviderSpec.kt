package com.uchuhimo.konf.source.toml

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.uchuhimo.konf.tempFileOf
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek

object TomlProviderSpec : SubjectSpek<TomlProvider>({
    subject { TomlProvider }

    given("a source provider") {
        on("create source from reader") {
            val source = subject.fromReader("type = \"reader\"".reader())
            it("should have correct type") {
                assertThat(source.info["type"], equalTo("TOML"))
            }
            it("should return a source which contains value from reader") {
                assertThat(source.get("type").toText(), equalTo("reader"))
            }
        }
        on("create source from input stream") {
            val source = subject.fromInputStream(
                    tempFileOf("type = \"inputStream\"").inputStream())
            it("should have correct type") {
                assertThat(source.info["type"], equalTo("TOML"))
            }
            it("should return a source which contains value from input stream") {
                assertThat(source.get("type").toText(), equalTo("inputStream"))
            }
        }
    }
})
