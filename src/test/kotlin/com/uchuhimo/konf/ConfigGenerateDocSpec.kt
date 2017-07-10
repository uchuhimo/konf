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
                        val size = optional("size", 1024, description = "size of disk file")
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
            if (item is LazyItem) {
                appendln("# default: ${item.placeholder}")
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
            toTree.visit(
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
            toTree.visit(
                    onEnterPath = { node ->
                        val path = node.path
                        if (path.size >= 1) {
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
                } else if (item is LazyItem) {
                    append("<!-- ${item.placeholder} -->")
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
