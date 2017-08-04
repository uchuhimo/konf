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
import com.uchuhimo.konf.source.SourceException
import com.uchuhimo.konf.source.toDuration
import java.time.DateTimeException
import java.time.Duration

object DurationDeserializer : StdDeserializer<Duration>(Duration::class.java) {
    override fun deserialize(parser: JsonParser, context: DeserializationContext): Duration? {
        when (parser.currentTokenId) {
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
        }
        throw context.mappingException("Expected type string.")
    }
}
