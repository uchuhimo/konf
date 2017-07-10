package com.uchuhimo.konf.source.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonTokenId
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import java.time.DateTimeException
import java.time.ZonedDateTime

object ZoneDateTimeDeserializer : JsonDeserializer<ZonedDateTime>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): ZonedDateTime? {
        when (parser.currentTokenId) {
            JsonTokenId.ID_STRING -> {
                val string = parser.text.trim({ it <= ' ' })
                if (string.isEmpty()) {
                    return null
                }
                try {
                    return ZonedDateTime.parse(string)
                } catch (e: DateTimeException) {
                    return rethrowDateTimeException<ZonedDateTime>(parser, context, e, string)
                }
            }
            JsonTokenId.ID_EMBEDDED_OBJECT -> return parser.embeddedObject as ZonedDateTime
        }
        throw context.mappingException("Expected type string.")
    }
}
