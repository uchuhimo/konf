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

package com.uchuhimo.konf.source.serializer

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import com.uchuhimo.konf.source.json.toJson
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.on
import org.junit.jupiter.api.assertDoesNotThrow
import java.io.StringReader
import java.io.StringWriter
import kotlin.test.assertEquals

object PrimitiveStdSerializerSpec : Spek({
    val config = Config {
        addSpec(WrappedStringSpec)
        mapper.registerModule(SimpleModule().apply {
            addSerializer(WrappedString::class.java, WrappedStringStdSerializer())
            addDeserializer(WrappedString::class.java, WrappedStringStdDeserializer())
        })
    }

    given("a config") {
        on("write json") {
            val writer = StringWriter()
            assertDoesNotThrow {
                config.toJson.toWriter(writer)
            }
            assert(writer.toString().contains("\"wrapped-string\" : \"value\""))
        }

        val json = """
            {
              "wrapped-string" : "1234"
            }
        """.trimIndent()
        given("json") {
            on("read json") {
                val read = config.from.json.reader(StringReader(json))
                assertEquals(WrappedString("1234"), read[WrappedStringSpec.wrappedString])
            }
        }
    }

})

private object WrappedStringSpec : ConfigSpec("") {

    val wrappedString by optional(name = "wrapped-string", default = WrappedString("value"))
}

private class WrappedStringStdSerializer : StdSerializer<WrappedString>(WrappedString::class.java) {

    override fun serialize(value: WrappedString, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeString(value.string)
    }
}

private class WrappedStringStdDeserializer : StdDeserializer<WrappedString>(WrappedString::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): WrappedString {
        return WrappedString(p.valueAsString)
    }
}

private data class WrappedString(val string: String)
