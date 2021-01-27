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

package com.uchuhimo.konf.source.properties

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.Writer
import com.uchuhimo.konf.source.base.toFlatMap
import java.io.FilterOutputStream
import java.io.OutputStream
import java.util.Properties

/**
 * Writer for properties source.
 */
class PropertiesWriter(val config: Config) : Writer {
    override fun toWriter(writer: java.io.Writer) {
        NoCommentProperties().apply { putAll(config.toFlatMap()) }.store(writer, null)
    }

    override fun toOutputStream(outputStream: OutputStream) {
        NoCommentProperties().apply { putAll(config.toFlatMap()) }.store(outputStream, null)
    }
}

private class NoCommentProperties : Properties() {
    private class StripFirstLineStream(out: OutputStream) : FilterOutputStream(out) {
        private var firstLineSeen = false

        override fun write(b: Int) {
            if (firstLineSeen) {
                super.write(b)
            } else if (b == '\n'.toInt()) {
                firstLineSeen = true
            }
        }
    }

    private class StripFirstLineWriter(writer: java.io.Writer) : java.io.FilterWriter(writer) {
        override fun write(cbuf: CharArray, off: Int, len: Int) {
            val offset = cbuf.indexOfFirst { it == '\n' }
            super.write(cbuf, offset + 1, len - offset - 1)
        }
    }

    override fun store(out: OutputStream, comments: String?) {
        super.store(StripFirstLineStream(out), null)
    }

    override fun store(writer: java.io.Writer, comments: String?) {
        super.store(StripFirstLineWriter(writer), null)
    }
}

/**
 * Returns writer for properties source.
 */
val Config.toProperties: Writer get() = PropertiesWriter(this)
