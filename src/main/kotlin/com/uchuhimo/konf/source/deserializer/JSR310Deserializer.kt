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
import com.fasterxml.jackson.core.JsonTokenId
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import java.time.format.DateTimeParseException

/**
 * Base class of deserializers for datetime classes in JSR310.
 *
 * @param T type of datetime value
 */
abstract class JSR310Deserializer<T>(clazz: Class<T>) : StdDeserializer<T>(clazz) {
    /**
     * Parses from a string to datetime value.
     *
     * @param string input string
     * @return datetime value
     * @throws DateTimeParseException
     */
    abstract fun parse(string: String): T

    final override fun deserialize(parser: JsonParser, context: DeserializationContext): T? {
        when (parser.currentTokenId) {
            JsonTokenId.ID_STRING -> {
                val string = parser.text.trim({ it <= ' ' })
                if (string.isEmpty()) {
                    return null
                }
                try {
                    return parse(string)
                } catch (exception: DateTimeParseException) {
                    throw context.weirdStringException(string, handledType(), exception.message).apply {
                        initCause(exception)
                    }
                }
            }
        }
        throw MismatchedInputException.from(parser, handledType(),
                "Unexpected token (${parser.currentToken}), expected string for ${handledType().name} value")
    }
}
