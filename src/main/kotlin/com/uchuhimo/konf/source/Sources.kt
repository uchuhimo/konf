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

package com.uchuhimo.konf.source

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.typesafe.config.impl.ConfigImplUtil
import com.uchuhimo.konf.getUnits
import com.uchuhimo.konf.source.deserializer.DurationDeserializer
import com.uchuhimo.konf.source.deserializer.OffsetDateTimeDeserializer
import com.uchuhimo.konf.source.deserializer.ZoneDateTimeDeserializer
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException
import java.util.concurrent.TimeUnit

fun Source.withFallback(fallback: Source): Source = object : Source by this {
    init {
        addInfo("fallback", fallback.description)
    }

    override fun contains(path: List<String>): Boolean =
            this@withFallback.contains(path) || fallback.contains(path)

    override fun get(path: List<String>): Source =
            this@withFallback.getOrNull(path) ?: fallback[path]

    override fun getOrNull(path: List<String>): Source? =
            this@withFallback.getOrNull(path) ?: fallback.getOrNull(path)

    override fun contains(key: String): Boolean =
            this@withFallback.contains(key) || fallback.contains(key)

    override fun get(key: String): Source =
            this@withFallback.getOrNull(key) ?: fallback[key]

    override fun getOrNull(key: String): Source? =
            this@withFallback.getOrNull(key) ?: fallback.getOrNull(key)
}

fun createDefaultMapper(): ObjectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule()
                .addDeserializer(Duration::class.java, DurationDeserializer)
                .addDeserializer(OffsetDateTime::class.java, OffsetDateTimeDeserializer)
                .addDeserializer(ZonedDateTime::class.java, ZoneDateTimeDeserializer))

fun Map<String, String>.toDescription() = map { (name, value) ->
    "$name: $value"
}.joinToString(separator = ", ", prefix = "[", postfix = "]")

fun String.toDuration(): Duration {
    try {
        return Duration.parse(this)
    } catch (e: DateTimeParseException) {
        return Duration.ofNanos(parseDuration(this))
    }
}

/**
 * Parses a duration string. If no units are specified in the string, it is
 * assumed to be in milliseconds. The returned duration is in nanoseconds.
 *
 * @param input the string to parse
 *
 * @return duration in nanoseconds
 */
internal fun parseDuration(input: String): Long {
    val s = ConfigImplUtil.unicodeTrim(input)
    val originalUnitString = getUnits(s)
    var unitString = originalUnitString
    val numberString = ConfigImplUtil.unicodeTrim(s.substring(0, s.length - unitString.length))

    // this would be caught later anyway, but the error message
    // is more helpful if we check it here.
    if (numberString.isEmpty())
        throw ParseException("No number in duration value '$input'")

    if (unitString.length > 2 && !unitString.endsWith("s"))
        unitString += "s"

    // note that this is deliberately case-sensitive
    val units = if (unitString == "" || unitString == "ms" || unitString == "millis"
            || unitString == "milliseconds") {
        TimeUnit.MILLISECONDS
    } else if (unitString == "us" || unitString == "micros" || unitString == "microseconds") {
        TimeUnit.MICROSECONDS
    } else if (unitString == "ns" || unitString == "nanos" || unitString == "nanoseconds") {
        TimeUnit.NANOSECONDS
    } else if (unitString == "d" || unitString == "days") {
        TimeUnit.DAYS
    } else if (unitString == "h" || unitString == "hours") {
        TimeUnit.HOURS
    } else if (unitString == "s" || unitString == "seconds") {
        TimeUnit.SECONDS
    } else if (unitString == "m" || unitString == "minutes") {
        TimeUnit.MINUTES
    } else {
        throw ParseException("Could not parse time unit '$originalUnitString' (try ns, us, ms, s, m, h, d)")
    }

    try {
        // if the string is purely digits, parse as an integer to avoid
        // possible precision loss;
        // otherwise as a double.
        if (numberString.matches("[+-]?[0-9]+".toRegex())) {
            return units.toNanos(java.lang.Long.parseLong(numberString))
        } else {
            val nanosInUnit = units.toNanos(1)
            return (java.lang.Double.parseDouble(numberString) * nanosInUnit).toLong()
        }
    } catch (e: NumberFormatException) {
        throw ParseException("Could not parse duration number '$numberString'")
    }
}
