package com.uchuhimo.konf.source.xml

import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.SourceProvider
import com.uchuhimo.konf.source.base.FlatSource
import org.dom4j.Document
import org.dom4j.io.SAXReader
import java.io.InputStream
import java.io.Reader

object XmlProvider : SourceProvider {
    private fun Document.toMap(): Map<String, String> {
        val rootElement = this.rootElement
        val propertyNodes = rootElement.selectNodes("/configuration/property")
        return mutableMapOf<String, String>().apply {
            for (property in propertyNodes) {
                put(property.selectSingleNode("name").text, property.selectSingleNode("value").text)
            }
        }
    }

    override fun fromReader(reader: Reader): Source {
        return FlatSource(SAXReader().read(reader).toMap(), type = "XML")
    }

    override fun fromInputStream(inputStream: InputStream): Source {
        return FlatSource(SAXReader().read(inputStream).toMap(), type = "XML")
    }
}
