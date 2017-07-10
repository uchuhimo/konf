package com.uchuhimo.konf.source

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import com.uchuhimo.konf.source.hocon.HoconProvider
import com.uchuhimo.konf.tempFileOf
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek
import spark.Spark.get
import spark.Spark.stop
import java.net.URL

object SourceProviderSpec : SubjectSpek<SourceProvider>({
    subject { HoconProvider }

    given("a source provider") {
        on("create source from reader") {
            val source = subject.fromReader("type = reader".reader())
            it("should have correct type") {
                assertThat(source.info["type"], equalTo("HOCON"))
            }
            it("should return a source which contains value from reader") {
                assertThat(source.get("type").toText(), equalTo("reader"))
            }
        }
        on("create source from input stream") {
            val source = subject.fromInputStream(
                    tempFileOf("type = inputStream").inputStream())
            it("should have correct type") {
                assertThat(source.info["type"], equalTo("HOCON"))
            }
            it("should return a source which contains value from input stream") {
                assertThat(source.get("type").toText(), equalTo("inputStream"))
            }
        }
        on("create source from file") {
            val file = tempFileOf("type = file")
            val source = subject.fromFile(file)
            it("should create from the specified file") {
                assertThat(source.context["file"], equalTo(file.toString()))
            }
            it("should return a source which contains value in file") {
                assertThat(source.get("type").toText(), equalTo("file"))
            }
        }
        on("create source from string") {
            val content = "type = string"
            val source = subject.fromString(content)
            it("should create from the specified string") {
                assertThat(source.context["content"], equalTo("\"\n$content\n\""))
            }
            it("should return a source which contains value in string") {
                assertThat(source.get("type").toText(), equalTo("string"))
            }
        }
        on("create source from byte array") {
            val source = subject.fromBytes("type = bytes".toByteArray())
            it("should return a source which contains value in byte array") {
                assertThat(source.get("type").toText(), equalTo("bytes"))
            }
        }
        on("create source from byte array slice") {
            val source = subject.fromBytes("|type = slice|".toByteArray(), 1, 12)
            it("should return a source which contains value in byte array slice") {
                assertThat(source.get("type").toText(), equalTo("slice"))
            }
        }
        on("create source from HTTP URL") {
            get("/source") { _, _ -> "type = http" }
            val url = "http://localhost:4567/source"
            val source = subject.fromUrl(URL(url))
            it("should create from the specified URL") {
                assertThat(source.context["url"], equalTo(url))
            }
            it("should return a source which contains value in URL") {
                assertThat(source.get("type").toText(), equalTo("http"))
            }
            stop()
        }
        on("create source from file URL") {
            val file = tempFileOf("type = fileUrl")
            val url = file.toURI().toURL()
            val source = subject.fromUrl(url)
            it("should create from the specified URL") {
                assertThat(source.context["url"], equalTo(url.toString()))
            }
            it("should return a source which contains value in URL") {
                assertThat(source.get("type").toText(), equalTo("fileUrl"))
            }
        }
        on("create source from resource") {
            val resource = "source/provider.conf"
            val source = subject.fromResource(resource)
            it("should create from the specified resource") {
                assertThat(source.context["resource"], equalTo(resource))
            }
            it("should return a source which contains value in resource") {
                assertThat(source.get("type").toText(), equalTo("resource"))
            }
        }
        on("create source from non-existed resource") {
            it("should throw SourceNotFoundException") {
                assertThat({ subject.fromResource("source/no-provider.conf") },
                        throws<SourceNotFoundException>())
            }
        }
    }
})
