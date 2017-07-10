package com.uchuhimo.konf.source.env

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek

object EnvProviderSpec : SubjectSpek<EnvProvider>({
    subject { EnvProvider }

    given("a source provider") {
        on("create source from system environment") {
            val source = subject.fromEnv()
            it("should have correct type") {
                assertThat(source.info["type"], equalTo("system-environment"))
            }
            it("should return a source which contains value from system environment") {
                assertThat(source.get(listOf("source", "test", "type")).toText(), equalTo("env"))
            }
        }
    }
})
