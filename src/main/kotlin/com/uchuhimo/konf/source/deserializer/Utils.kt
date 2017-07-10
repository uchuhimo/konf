package com.uchuhimo.konf.source.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonMappingException
import java.time.DateTimeException
import java.time.format.DateTimeParseException

@Throws(JsonMappingException::class)
internal fun <BOGUS> JsonDeserializer<*>.rethrowDateTimeException(
        p: JsonParser,
        context: DeserializationContext,
        e0: DateTimeException, value: String
): BOGUS {
    val e: JsonMappingException
    if (e0 is DateTimeParseException) {
        e = context.weirdStringException(value, handledType(), e0.message)
        e.initCause(e0)
    } else {
        e = JsonMappingException.from(p,
                String.format("Failed to deserialize %s: (%s) %s",
                        handledType().name, e0.javaClass.name, e0.message), e0)
    }
    throw e
}
