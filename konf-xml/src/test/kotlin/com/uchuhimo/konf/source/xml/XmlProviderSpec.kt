/*
 * Copyright 2017-2019 the original author or authors.
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

package com.uchuhimo.konf.source.xml

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.uchuhimo.konf.source.asValue
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
        """.replaceIndent()

    given("a XML provider") {
        on("create source from reader") {
            val source = subject.fromReader(xmlDoc("type", "reader").reader())
            it("should have correct type") {
                assertThat(source.info["type"], equalTo("XML"))
            }
            it("should return a source which contains value from reader") {
                assertThat(source["type"].asValue<String>(), equalTo("reader"))
            }
        }
        on("create source from input stream") {
            val source = subject.fromInputStream(
                tempFileOf(xmlDoc("type", "inputStream")).inputStream())
            it("should have correct type") {
                assertThat(source.info["type"], equalTo("XML"))
            }
            it("should return a source which contains value from input stream") {
                assertThat(source["type"].asValue<String>(), equalTo("inputStream"))
            }
        }
    }
})
