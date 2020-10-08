/*
 * Copyright 2017-2020 the original author or authors.
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

package com.uchuhimo.konf.source.yaml

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import com.uchuhimo.konf.source.Writer
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek
import java.io.ByteArrayOutputStream
import java.io.StringWriter

object YamlWriterSpec : SubjectSpek<Writer>({
    subject {
        val config = Config {
            addSpec(
                object : ConfigSpec() {
                    val key by optional("value")
                }
            )
        }
        config.toYaml
    }

    given("a writer") {
        val expectedString = "key: value" + System.lineSeparator()
        on("save to writer") {
            val writer = StringWriter()
            subject.toWriter(writer)
            it("should return a writer which contains content from config") {
                assertThat(writer.toString(), equalTo(expectedString))
            }
        }
        on("save to output stream") {
            val outputStream = ByteArrayOutputStream()
            subject.toOutputStream(outputStream)
            it("should return an output stream which contains content from config") {
                assertThat(outputStream.toString(), equalTo(expectedString))
            }
        }
    }
})
