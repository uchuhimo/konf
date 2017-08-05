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

package com.uchuhimo.konf.source.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonMappingException
import java.io.IOException
import java.time.DateTimeException
import java.time.format.DateTimeParseException
import java.util.Arrays

@Throws(JsonMappingException::class)
internal fun <BOGUS> JsonDeserializer<*>.rethrowDateTimeException(
        context: DeserializationContext,
        exception: DateTimeException, value: String): BOGUS {
    if (exception is DateTimeParseException) {
        throw context.weirdStringException(value, handledType(), exception.message).apply {
            initCause(exception)
        }
    }
    if (exception is DateTimeException) {
        val message = exception.message
        if (message != null && message.contains("invalid format")) {
            throw context.weirdStringException(value, handledType(), exception.message).apply {
                initCause(exception)
            }
        }
    }
    return context.reportInputMismatch<BOGUS>(handledType(),
            "Failed to deserialize %s: (%s) %s",
            handledType().name, exception.javaClass.name, exception.message)
}

@Throws(IOException::class)
internal fun <BOGUS> JsonDeserializer<*>.reportWrongToken(
        parser: JsonParser,
        context: DeserializationContext,
        vararg expTypes: JsonToken
): BOGUS {
    return context.reportInputMismatch<BOGUS>(handledType(),
            "Unexpected token (%s), expected one of %s for %s value",
            parser.currentToken,
            Arrays.asList(*expTypes).toString(),
            handledType().name)
}
