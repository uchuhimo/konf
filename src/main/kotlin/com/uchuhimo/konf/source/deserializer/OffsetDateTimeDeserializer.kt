package com.uchuhimo.konf.source.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonTokenId
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import java.time.DateTimeException
import java.time.OffsetDateTime

object OffsetDateTimeDeserializer : JsonDeserializer<OffsetDateTime>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): OffsetDateTime? {
        when (parser.currentTokenId) {
            JsonTokenId.ID_STRING -> {
                val string = parser.text.trim({ it <= ' ' })
                if (string.isEmpty()) {
                    return null
                }
                try {
                    return OffsetDateTime.parse(string)
                } catch (e: DateTimeException) {
                    return rethrowDateTimeException<OffsetDateTime>(parser, context, e, string)
                }
            }
            JsonTokenId.ID_EMBEDDED_OBJECT -> return parser.embeddedObject as OffsetDateTime
        }
        throw context.mappingException("Expected type string.")
    }
}
