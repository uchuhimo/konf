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

package com.uchuhimo.konf.source.hocon

import com.typesafe.config.ConfigRenderOptions
import com.typesafe.config.ConfigValueFactory
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.Writer
import com.uchuhimo.konf.source.base.toHierarchicalMap
import java.io.OutputStream

/**
 * Writer for HOCON source.
 */
class HoconWriter(val config: Config) : Writer {
    private val renderOpts = ConfigRenderOptions.defaults()
        .setOriginComments(false)
        .setComments(false)
        .setJson(false)

    override fun toWriter(writer: java.io.Writer) {
        writer.write(toText())
    }

    override fun toOutputStream(outputStream: OutputStream) {
        outputStream.writer().use {
            toWriter(it)
        }
    }

    override fun toText(): String {
        return ConfigValueFactory.fromMap(config.toHierarchicalMap()).render(renderOpts)
            .replace("\n", System.lineSeparator())
    }
}

/**
 * Returns writer for HOCON source.
 */
val Config.toHocon: Writer get() = HoconWriter(this)
