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

package com.uchuhimo.konf.source.yaml

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.Feature
import com.uchuhimo.konf.ListNode
import com.uchuhimo.konf.TreeNode
import com.uchuhimo.konf.ValueNode
import com.uchuhimo.konf.source.Writer
import java.io.OutputStream

/**
 * Writer for YAML source.
 */
class YamlWriter(val config: Config) : Writer {

    override fun toWriter(writer: java.io.Writer) {
        val nodeWriter = YamlTreeNodeWriter(writer, config.isEnabled(Feature.WRITE_DESCRIPTIONS_AS_COMMENTS))
        nodeWriter.write(config.toTree())
    }

    override fun toOutputStream(outputStream: OutputStream) {
        outputStream.writer().use {
            toWriter(it)
        }
    }
}

private class YamlTreeNodeWriter(
    private val writer: java.io.Writer,
    private val writeComments: Boolean = false
) {

    private val indentSize = 2
    private var ident = 0

    private fun increaseIndent() {
        this.ident += this.indentSize
    }

    private fun decreaseIndent() {
        this.ident -= this.indentSize
    }

    private fun writeIndent() {
        repeat(this.ident) {
            this.writer.write(' '.toInt())
        }
    }

    private fun write(char: Char) {
        this.writer.write(char.toInt())
    }

    private fun write(string: String) {
        this.writer.write(string)
    }

    private fun writeNewLine() {
        write('\n')
    }

    fun write(node: TreeNode) {
        write(node, false)
    }

    private fun write(node: TreeNode, inList: Boolean = false) {
        when (node) {
            is ValueNode -> writeValue(node)
            is ListNode -> writeList(node, inList)
            else -> writeMap(node, inList)
        }
    }

    private fun writeComments(node: TreeNode) {
        if (!this.writeComments || node.comments.isEmpty())
            return
        val comments = node.comments.split("\n")
        comments.forEach { comment ->
            writeIndent()
            write("# $comment")
            writeNewLine()
        }
    }

    private fun shouldWriteComments(node: TreeNode) = this.writeComments && node.comments.isNotEmpty()

    private fun writeValue(node: ValueNode) {
        writeStringValue(node.value.toString())
    }

    private fun writeStringValue(string: String) {
        val lines = string.split("\n")
        if (lines.size > 1) {
            // Multiline
            write('|')
            writeNewLine()
            increaseIndent()
            lines.forEach { line ->
                writeIndent()
                write(line)
                writeNewLine()
            }
            decreaseIndent()
        } else {
            write(quoteValueIfNeeded(string))
            writeNewLine()
        }
    }

    private fun writeList(node: ListNode, inList: Boolean = false) {
        val list = node.list
        if (list.isEmpty()) {
            write(" []")
            writeNewLine()
        } else {
            increaseIndent()
            var first = true
            list.forEach { element ->
                val firstListInListEntry = first && inList && !shouldWriteComments(list[0])
                if (!firstListInListEntry) {
                    if (first)
                        writeNewLine()
                    writeComments(element)
                    writeIndent()
                }
                first = false
                write("- ")
                write(element, inList = true)
            }
            decreaseIndent()
        }
    }

    private fun writeMap(node: TreeNode, inList: Boolean = false) {
        val map = node.children
        if (map.isEmpty()) {
            write(" {}")
            writeNewLine()
        } else {
            var first = true
            if (inList)
                increaseIndent()
            map.forEach { (name, node) ->
                writeEntry(name, node, inList, first)
                first = false
            }
            if (inList)
                decreaseIndent()
        }
    }

    private fun quoteString(s: String) = "\"${s.replace("\"", "\\\"")}\""

    private fun hasQuoteChar(s: String) = '\"' in s || '\'' in s

    private fun hasTrailingWhitespace(s: String) = s.isNotEmpty() && (s.first() == ' ' || s.last() == ' ')

    private fun quoteValueIfNeeded(s: String): String {
        if (s.isEmpty())
            return s
        if (s.last() == ':' || hasTrailingWhitespace(s) || hasQuoteChar(s))
            return quoteString(s)
        return s
    }

    private fun writeEntry(name: String, node: TreeNode, first: Boolean = false, inList: Boolean = false) {
        val firstListEntry = first && inList
        if (!firstListEntry || shouldWriteComments(node)) {
            if (firstListEntry)
                writeNewLine()
            writeComments(node)
            writeIndent()
        }
        write(quoteValueIfNeeded(name))
        write(':')
        when (node) {
            is ValueNode -> {
                write(' ')
                writeValue(node)
            }
            is ListNode -> {
                writeList(node)
            }
            else -> {
                writeNewLine()
                increaseIndent()
                writeMap(node)
                decreaseIndent()
            }
        }
    }
}

/**
 * Returns writer for YAML source.
 */
val Config.toYaml: Writer get() = YamlWriter(this)
