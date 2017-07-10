package com.uchuhimo.konf.source

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import com.uchuhimo.konf.tempFileOf
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek
import spark.Spark
import java.net.URL

object LoaderSpec : SubjectSpek<Loader>({
    subject {
        Config {
            addSpec(SourceType)
        }.loadFrom.hocon
    }

    given("a loader") {
        on("load from reader") {
            val config = subject.reader("type = reader".reader())
            it("should return a config which contains value from reader") {
                assertThat(config[SourceType.type], equalTo("reader"))
            }
        }
        on("load from input stream") {
            val config = subject.inputStream(
                    tempFileOf("type = inputStream").inputStream())
            it("should return a config which contains value from input stream") {
                assertThat(config[SourceType.type], equalTo("inputStream"))
            }
        }
        on("load from file") {
            val config = subject.file(tempFileOf("type = file"))
            it("should return a config which contains value in file") {
                assertThat(config[SourceType.type], equalTo("file"))
            }
        }
        on("load from string") {
            val config = subject.string("type = string")
            it("should return a config which contains value in string") {
                assertThat(config[SourceType.type], equalTo("string"))
            }
        }
        on("load from byte array") {
            val config = subject.bytes("type = bytes".toByteArray())
            it("should return a config which contains value in byte array") {
                assertThat(config[SourceType.type], equalTo("bytes"))
            }
        }
        on("load from byte array slice") {
            val config = subject.bytes("|type = slice|".toByteArray(), 1, 12)
            it("should return a config which contains value in byte array slice") {
                assertThat(config[SourceType.type], equalTo("slice"))
            }
        }
        on("load from HTTP URL") {
            Spark.get("/source") { _, _ -> "type = http" }
            val config = subject.url(URL("http://localhost:4567/source"))
            it("should return a config which contains value in URL") {
                assertThat(config[SourceType.type], equalTo("http"))
            }
            Spark.stop()
        }
        on("load from file URL") {
            val file = tempFileOf("type = fileUrl")
            val config = subject.url(file.toURI().toURL())
            it("should return a config which contains value in URL") {
                assertThat(config[SourceType.type], equalTo("fileUrl"))
            }
        }
        on("load from resource") {
            val config = subject.resource("source/provider.conf")
            it("should return a config which contains value in resource") {
                assertThat(config[SourceType.type], equalTo("resource"))
            }
        }
        on("load from non-existed resource") {
            it("should throw SourceNotFoundException") {
                assertThat({ subject.resource("source/no-provider.conf") },
                        throws<SourceNotFoundException>())
            }
        }
    }
})

private object SourceType : ConfigSpec() {
    val type = required<String>("type")
}
