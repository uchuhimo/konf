/*
 * Copyright 2017-2018 the original author or authors.
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

package com.uchuhimo.konf

import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.api.dsl.xgiven
import org.jetbrains.spek.subject.SubjectSpek

object ConfigGenerateDocSpec : SubjectSpek<Config>({

    val spec = NetworkBuffer

    subject { Config { addSpec(spec) } }

    xgiven("a config") {
        group("generate doc") {
            val complexConfig by memoized {
                subject.apply {
                    addSpec(object : ConfigSpec("disk.file") {
                        init {
                            optional("size", 1024, description = "size of disk file")
                        }
                    })
                }
            }
            on("generate Java properties doc") {
                it("generate doc in correct format") {
                    println(complexConfig.generatePropertiesDoc())
                }
            }
            on("generate HOCON doc") {
                it("generate doc in correct format") {
                    println(complexConfig.generateHoconDoc())
                }
            }
            on("generate YAML doc") {
                it("generate doc in correct format") {
                    println(complexConfig.generateYamlDoc())
                }
            }
            on("generate TOML doc") {
                it("generate doc in correct format") {
                    println(complexConfig.generateTomlDoc())
                }
            }
            on("generate XML doc") {
                it("generate doc in correct format") {
                    println(complexConfig.generateXmlDoc())
                }
            }
        }
    }
})

private fun generateItemDoc(
        item: Item<*>,
        key: String = item.name,
        separator: String = " = ",
        encode: (Any) -> String
): String =
        StringBuilder().apply {
            item.description.lines().forEach { line ->
                appendln("# $line")
            }
            append(key)
            append(separator)
            if (item is OptionalItem) {
                append(encode(item.default))
            }
            appendln()
        }.toString()

fun Config.generatePropertiesDoc(): String =
        StringBuilder().apply {
            for (item in this@generatePropertiesDoc) {
                append(generateItemDoc(item, encode = Any::toString))
                appendln()
            }
        }.toString()

private fun encodeAsHocon(value: Any): String =
        when (value) {
            is Int -> value.toString()
            is String -> "\"$value\""
            is Enum<*> -> "\"${value.name}\""
            else -> value.toString()
        }

fun Config.generateHoconDoc(): String =
        StringBuilder().apply {
            toTree().visit(
                    onEnterPath = { node ->
                        val path = node.path
                        if (path.isNotEmpty()) {
                            append(" ".repeat(4 * (path.size - 1)))
                            appendln("${path.last()} {")
                        }
                    },
                    onLeavePath = { node ->
                        val path = node.path
                        if (path.isNotEmpty()) {
                            append(" ".repeat(4 * (path.size - 1)))
                            appendln("}")
                        }
                    },
                    onEnterItem = { node ->
                        val path = node.path
                        val item = node.item
                        generateItemDoc(
                                item,
                                key = item.path.last(),
                                encode = ::encodeAsHocon
                        ).lines().forEach { line ->
                            append(" ".repeat(4 * (path.size - 1)))
                            append(line)
                            appendln()
                        }
                    }
            )
        }.toString()

fun Config.generateYamlDoc(): String =
        StringBuilder().apply {
            toTree().visit(
                    onEnterPath = { node ->
                        val path = node.path
                        if (path.isNotEmpty()) {
                            append(" ".repeat(4 * (path.size - 1)))
                            appendln("${path.last()}:")
                        }
                    },
                    onEnterItem = { node ->
                        val item = node.item
                        val path = node.path
                        generateItemDoc(
                                item,
                                key = item.path.last(),
                                separator = ": ",
                                encode = ::encodeAsHocon
                        ).lines().forEach { line ->
                            append(" ".repeat(4 * (path.size - 1)))
                            append(line)
                            appendln("")
                        }
                    }
            )
        }.toString()

fun Config.generateTomlDoc(): String =
        StringBuilder().apply {
            specs.forEach { spec ->
                appendln("[${spec.prefix}]")
                spec.items.forEach { item ->
                    append(generateItemDoc(item, key = item.path.last(), encode = ::encodeAsHocon))
                    appendln()
                }
            }
        }.toString()

fun Config.generateXmlDoc(): String =
        StringBuilder().apply {
            appendln("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            appendln("<configuration>")
            for (item in this@generateXmlDoc) {
                appendln("  <property>")
                appendln("    <name>${item.name}</name>")
                append("    <value>")
                if (item is OptionalItem) {
                    append(item.default.toString())
                }
                appendln("</value>")
                appendln("    <description>")
                item.description.lines().forEach { line ->
                    appendln("      $line")
                }
                appendln("    </description>")
                appendln("  </property>")
            }
            appendln("</configuration>")
        }.toString()
