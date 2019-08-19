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

package com.uchuhimo.konf.source.toml

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.Writer
import com.uchuhimo.konf.source.base.toHierarchicalMap
import java.io.OutputStream
import com.moandjiezana.toml.TomlWriter as Toml4jWriter

/**
 * Writer for TOML source.
 */
class TomlWriter(val config: Config) : Writer {
    private val toml4jWriter = Toml4jWriter.Builder()
        .indentValuesBy(0)
        .indentTablesBy(0)
        .build()

    override fun toWriter(writer: java.io.Writer) {
        writer.write(toText())
    }

    override fun toOutputStream(outputStream: OutputStream) {
        outputStream.writer().use {
            toWriter(it)
        }
    }

    override fun toText(): String {
        // TODO: fix bug that map in nested list is formatted incorrectly (open issue in toml4j)
        return toml4jWriter.write(config.toHierarchicalMap()).replace("\n", System.lineSeparator())
    }
}

/**
 * Returns writer for TOML source.
 */
val Config.toToml: Writer get() = TomlWriter(this)
