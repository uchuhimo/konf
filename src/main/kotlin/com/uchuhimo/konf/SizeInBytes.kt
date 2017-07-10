package com.uchuhimo.konf

import com.fasterxml.jackson.annotation.JsonCreator
import com.typesafe.config.impl.ConfigImplUtil
import com.uchuhimo.konf.source.ParseException
import java.math.BigDecimal
import java.math.BigInteger

data class SizeInBytes(val bytes: Long) {
    init {
        check(bytes >= 0)
    }

    companion object {
        /**
         * Parses a size-in-bytes string. If no units are specified in the string,
         * it is assumed to be in bytes. The returned value is in bytes.
         *
         * @param input the string to parse
         *
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

        private enum class MemoryUnit constructor(
                internal val prefix: String,
                internal val powerOf: Int,
                internal val power: Int
        ) {
            BYTES("", 1024, 0),

            KILOBYTES("kilo", 1000, 1),
            MEGABYTES("mega", 1000, 2),
            GIGABYTES("giga", 1000, 3),
            TERABYTES("tera", 1000, 4),
            PETABYTES("peta", 1000, 5),
            EXABYTES("exa", 1000, 6),
            ZETTABYTES("zetta", 1000, 7),
            YOTTABYTES("yotta", 1000, 8),

            KIBIBYTES("kibi", 1024, 1),
            MEBIBYTES("mebi", 1024, 2),
            GIBIBYTES("gibi", 1024, 3),
            TEBIBYTES("tebi", 1024, 4),
            PEBIBYTES("pebi", 1024, 5),
            EXBIBYTES("exbi", 1024, 6),
            ZEBIBYTES("zebi", 1024, 7),
            YOBIBYTES("yobi", 1024, 8);

            internal val bytes: BigInteger = BigInteger.valueOf(powerOf.toLong()).pow(power)

            companion object {

                private fun makeUnitsMap(): Map<String, MemoryUnit> {
                    val map = java.util.HashMap<String, MemoryUnit>()
                    for (unit in MemoryUnit.values()) {
                        map.put(unit.prefix + "byte", unit)
                        map.put(unit.prefix + "bytes", unit)
                        if (unit.prefix.isEmpty()) {
                            map.put("b", unit)
                            map.put("B", unit)
                            map.put("", unit) // no unit specified means bytes
                        } else {
                            val first = unit.prefix.substring(0, 1)
                            val firstUpper = first.toUpperCase()
                            if (unit.powerOf == 1024) {
                                map.put(first, unit)             // 512m
                                map.put(firstUpper, unit)        // 512M
                                map.put(firstUpper + "i", unit)  // 512Mi
                                map.put(firstUpper + "iB", unit) // 512MiB
                            } else if (unit.powerOf == 1000) {
                                if (unit.power == 1) {
                                    map.put(first + "B", unit) // 512kB
                                } else {
                                    map.put(firstUpper + "B", unit) // 512MB
                                }
                            } else {
                                throw RuntimeException("broken MemoryUnit enum")
                            }
                        }
                    }
                    return map
                }

                private val unitsMap = makeUnitsMap()

                internal fun parseUnit(unit: String): MemoryUnit? {
                    return unitsMap[unit]
                }
            }
        }
    }
}

fun String.toSizeInBytes(): SizeInBytes = SizeInBytes.parse(this)
