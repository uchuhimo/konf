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

package com.uchuhimo.konf.source.xml

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.Writer
import com.uchuhimo.konf.source.base.toFlatMap
import org.dom4j.Document
import org.dom4j.DocumentHelper
import org.dom4j.io.OutputFormat
import org.dom4j.io.XMLWriter
import java.io.OutputStream

/**
 * Writer for XML source.
 */
class XmlWriter(val config: Config) : Writer {
    private fun Map<String, String>.toDocument(): Document {
        val document = DocumentHelper.createDocument()
        val rootElement = document.addElement("configuration")
        for ((key, value) in this) {
            val propertyElement = rootElement.addElement("property")
            propertyElement.addElement("name").text = key
            propertyElement.addElement("value").text = value
        }
        return document
    }

    override fun toWriter(writer: java.io.Writer) {
        val xmlWriter = XMLWriter(writer, OutputFormat.createPrettyPrint())
        xmlWriter.write(config.toFlatMap().toDocument())
        xmlWriter.close()
    }

    override fun toOutputStream(outputStream: OutputStream) {
        val xmlWriter = XMLWriter(outputStream, OutputFormat.createPrettyPrint())
        xmlWriter.write(config.toFlatMap().toDocument())
        xmlWriter.close()
    }
}

/**
 * Returns writer for XML source.
 */
val Config.toXml: Writer get() = XmlWriter(this)
