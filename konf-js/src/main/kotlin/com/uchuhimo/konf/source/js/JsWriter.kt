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

package com.uchuhimo.konf.source.js

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.ObjectMapper
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.Writer
import com.uchuhimo.konf.source.base.toHierarchicalMap
import java.io.OutputStream
import java.util.UUID
import java.util.regex.Pattern

class MarkedPrettyPrinter(
    mapper: ObjectMapper,
    val mark: String
) : DefaultPrettyPrinter(mapper.serializationConfig.defaultPrettyPrinter as DefaultPrettyPrinter) {
    override fun writeObjectFieldValueSeparator(g: JsonGenerator) {
        g.writeRaw(mark)
    }
}

/**
 * Writer for JavaScript source.
 */
class JsWriter(val config: Config) : Writer {
    override fun toWriter(writer: java.io.Writer) {
        val mark = UUID.randomUUID().toString()
        val jsonOutput = config.mapper
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(config.toHierarchicalMap())
        val p = Pattern.compile("(\")(.*)(\"\\s*):")
        val jsOutput = p.matcher(jsonOutput).replaceAll("$2:")
        writer.write("($jsOutput)")
    }

    override fun toOutputStream(outputStream: OutputStream) {
        outputStream.writer().use {
            toWriter(it)
        }
    }
}

/**
 * Returns writer for JavaScript source.
 */
val Config.toJs: Writer get() = JsWriter(this)
