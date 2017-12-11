/*
 * Copyright 2017 the original author or authors.
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
import java.io.OutputStream
import java.util.Properties

/**
 * Provider for properties source.
 */
class PropertiesWriter(val config: Config) : Writer {
    override fun toWriter(writer: java.io.Writer) {
        Properties().apply { putAll(config.toFlatMap()) }.store(writer, null)
    }

    override fun toOutputStream(outputStream: OutputStream) {
        Properties().apply { putAll(config.toFlatMap()) }.store(outputStream, null)
    }
}

val Config.toProperties: Writer get() = PropertiesWriter(this)
