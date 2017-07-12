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

import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.SourceProvider
import com.uchuhimo.konf.source.base.FlatSource
import java.io.InputStream
import java.io.Reader
import java.util.Properties

object PropertiesProvider : SourceProvider {
    private fun Properties.toMap(): Map<String, String> {
        return mapKeys {
            it.key as String
        }.mapValues {
            it.value as String
        }
    }

    override fun fromReader(reader: Reader): Source =
            FlatSource(Properties().apply { load(reader) }.toMap(), type = "properties")

    override fun fromInputStream(inputStream: InputStream): Source =
            FlatSource(Properties().apply { load(inputStream) }.toMap(), type = "properties")

    fun fromSystem(): Source = FlatSource(System.getProperties().toMap(), type = "system-properties")
}
