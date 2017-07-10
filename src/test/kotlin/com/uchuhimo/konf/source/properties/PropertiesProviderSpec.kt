package com.uchuhimo.konf.source.properties

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.uchuhimo.konf.tempFileOf
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek

object PropertiesProviderSpec : SubjectSpek<PropertiesProvider>({
    subject { PropertiesProvider }

    given("a source provider") {
        on("create source from reader") {
            val source = subject.fromReader("type = reader".reader())
            it("should have correct type") {
                assertThat(source.info["type"], equalTo("properties"))
            }
            it("should return a source which contains value from reader") {
                assertThat(source.get("type").toText(), equalTo("reader"))
            }
        }
        on("create source from input stream") {
            val source = subject.fromInputStream(
                    tempFileOf("type = inputStream").inputStream())
            it("should have correct type") {
                assertThat(source.info["type"], equalTo("properties"))
            }
            it("should return a source which contains value from input stream") {
                assertThat(source.get("type").toText(), equalTo("inputStream"))
            }
        }
        on("create source from system properties") {
            System.setProperty("type", "system")
            val source = subject.fromSystem()
            it("should have correct type") {
                assertThat(source.info["type"], equalTo("system-properties"))
            }
            it("should return a source which contains value from system properties") {
                assertThat(source.get("type").toText(), equalTo("system"))
            }
        }
    }
})
