/*
 * Copyright 2017-2021 the original author or authors.
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

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.Writer
import com.uchuhimo.konf.source.base.toHierarchicalMap
import java.io.OutputStream
import java.util.regex.Pattern

/**
 * Writer for JavaScript source.
 */
class JsWriter(val config: Config) : Writer {
    override fun toWriter(writer: java.io.Writer) {
        val jsonOutput = config.mapper
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(config.toHierarchicalMap())
        val pattern = Pattern.compile("(\")(.*)(\"\\s*):")
        val jsOutput = pattern.matcher(jsonOutput).replaceAll("$2:")
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
