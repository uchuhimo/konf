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

package com.uchuhimo.konf

import com.fasterxml.jackson.annotation.JsonCreator
import com.typesafe.config.impl.ConfigImplUtil
import com.uchuhimo.konf.source.ParseException
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Represents size in unit of bytes.
 */
data class SizeInBytes(
        /**
         * Number of bytes.
         */
        val bytes: Long) {
    init {
        require(bytes >= 0)
    }

    companion object {
        /**
         * Parses a size-in-bytes string. If no units are specified in the string,
         * it is assumed to be in bytes. The returned value is in bytes.
         *
         * @param input the string to parse
         * @return size in bytes
         */
        @JsonCreator
        @JvmStatic
        fun parse(input: String): SizeInBytes {
            val s = ConfigImplUtil.unicodeTrim(input)
            val unitString = getUnits(s)
            val numberString = ConfigImplUtil.unicodeTrim(s.substring(0,
                    s.length - unitString.length))

            // this would be caught later anyway, but the error message
            // is more helpful if we check it here.
            if (numberString.isEmpty())
                throw ParseException("No number in size-in-bytes value '$input'")

            val units = MemoryUnit.parseUnit(unitString) ?:
                    throw ParseException("Could not parse size-in-bytes unit '$unitString'" +
                            " (try k, K, kB, KiB, kilobytes, kibibytes)")

            try {
                val result: BigInteger
                // if the string is purely digits, parse as an integer to avoid
                // possible precision loss; otherwise as a double.
                if (numberString.matches("[0-9]+".toRegex())) {
                    result = units.bytes.multiply(BigInteger(numberString))
                } else {
                    val resultDecimal = BigDecimal(units.bytes).multiply(BigDecimal(numberString))
                    result = resultDecimal.toBigInteger()
                }
                if (result.bitLength() < 64)
                    return SizeInBytes(result.toLong())
                else
                    throw ParseException("size-in-bytes value is out of range for a 64-bit long: '$input'")
            } catch (e: NumberFormatException) {
                throw ParseException("Could not parse size-in-bytes number '$numberString'")
            }
        }

        private enum class Radix {
            KILO {
                override fun toInt(): Int = 1000
            },
            KIBI {
                override fun toInt(): Int = 1024
            };

            abstract fun toInt(): Int
        }

        private enum class MemoryUnit(
                private val prefix: String,
                private val radix: Radix,
                private val power: Int
        ) {
            BYTES("", Radix.KIBI, 0),

            KILOBYTES("kilo", Radix.KILO, 1),
            MEGABYTES("mega", Radix.KILO, 2),
            GIGABYTES("giga", Radix.KILO, 3),
            TERABYTES("tera", Radix.KILO, 4),
            PETABYTES("peta", Radix.KILO, 5),
            EXABYTES("exa", Radix.KILO, 6),
            ZETTABYTES("zetta", Radix.KILO, 7),
            YOTTABYTES("yotta", Radix.KILO, 8),

            KIBIBYTES("kibi", Radix.KIBI, 1),
            MEBIBYTES("mebi", Radix.KIBI, 2),
            GIBIBYTES("gibi", Radix.KIBI, 3),
            TEBIBYTES("tebi", Radix.KIBI, 4),
            PEBIBYTES("pebi", Radix.KIBI, 5),
            EXBIBYTES("exbi", Radix.KIBI, 6),
            ZEBIBYTES("zebi", Radix.KIBI, 7),
            YOBIBYTES("yobi", Radix.KIBI, 8);

            internal val bytes: BigInteger = BigInteger.valueOf(radix.toInt().toLong()).pow(power)

            companion object {

                private val unitsMap = mutableMapOf<String, MemoryUnit>().apply {
                    for (unit in MemoryUnit.values()) {
                        put(unit.prefix + "byte", unit)
                        put(unit.prefix + "bytes", unit)
                        if (unit.prefix.isEmpty()) {
                            put("b", unit)
                            put("B", unit)
                            put("", unit) // no unit specified means bytes
                        } else {
                            val first = unit.prefix.substring(0, 1)
                            val firstUpper = first.toUpperCase()
                            when (unit.radix) {
                                Radix.KILO -> {
                                    if (unit.power == 1) {
                                        put(first + "B", unit) // 512kB
                                    } else {
                                        put(firstUpper + "B", unit) // 512MB
                                    }
                                }
                                Radix.KIBI -> {
                                    put(first, unit)             // 512m
                                    put(firstUpper, unit)        // 512M
                                    put(firstUpper + "i", unit)  // 512Mi
                                    put(firstUpper + "iB", unit) // 512MiB
                                }
                            }
                        }
                    }
                }

                internal fun parseUnit(unit: String): MemoryUnit? {
                    return unitsMap[unit]
                }
            }
        }
    }
}

/**
 * Converts a string to [SizeInBytes].
 */
fun String.toSizeInBytes(): SizeInBytes = SizeInBytes.parse(this)
