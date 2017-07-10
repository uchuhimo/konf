package com.uchuhimo.konf.source.xml

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.uchuhimo.konf.tempFileOf
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek

object XmlProviderSpec : SubjectSpek<XmlProvider>({
    subject { XmlProvider }

    fun xmlDoc(name: String, value: String) = """
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <property>
        <name>$name</name>
        <value>$value</value>
    </property>
</configuration>
""".trim()

    given("a source provider") {
        on("create source from reader") {
            val source = subject.fromReader(xmlDoc("type", "reader").reader())
            it("should have correct type") {
                assertThat(source.info["type"], equalTo("XML"))
            }
            it("should return a source which contains value from reader") {
                assertThat(source.get("type").toText(), equalTo("reader"))
            }
        }
        on("create source from input stream") {
            val source = subject.fromInputStream(
                    tempFileOf(xmlDoc("type", "inputStream")).inputStream())
            it("should have correct type") {
                assertThat(source.info["type"], equalTo("XML"))
            }
            it("should return a source which contains value from input stream") {
                assertThat(source.get("type").toText(), equalTo("inputStream"))
            }
        }
    }
})
