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
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.datatype.jsr310.DecimalUtils
import com.uchuhimo.konf.source.SourceException
import com.uchuhimo.konf.source.toDuration
import java.time.DateTimeException
import java.time.Duration

object DurationDeserializer : JsonDeserializer<Duration>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): Duration? {
        when (parser.currentTokenId) {
            JsonTokenId.ID_NUMBER_FLOAT -> {
                val value = parser.decimalValue
                val seconds = value.toLong()
                val nanoseconds = DecimalUtils.extractNanosecondDecimal(value, seconds)
                return Duration.ofSeconds(seconds, nanoseconds.toLong())
            }

            JsonTokenId.ID_NUMBER_INT -> {
                if (context.isEnabled(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS)) {
                    return Duration.ofSeconds(parser.longValue)
                }
                return Duration.ofMillis(parser.longValue)
            }

            JsonTokenId.ID_STRING -> {
                val string = parser.text.trim({ it <= ' ' })
                if (string.isEmpty()) {
                    return null
                }
                try {
                    return Duration.parse(string)
                } catch (e: DateTimeException) {
                    try {
                        return string.toDuration()
                    } catch (_: SourceException) {
                        return rethrowDateTimeException<Duration>(parser, context, e, string)
                    }
                }
            }
            JsonTokenId.ID_EMBEDDED_OBJECT -> return parser.embeddedObject as Duration
        }
        throw context.mappingException("Expected type float, integer, or string.")
    }

}
