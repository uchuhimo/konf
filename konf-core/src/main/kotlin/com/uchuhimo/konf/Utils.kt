/*
 * Copyright 2017-2021 the original author or authors.
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

import java.io.File

/**
 * Throws [UnsupportedOperationException].
 *
 * @throws UnsupportedOperationException
 */
@Suppress("NOTHING_TO_INLINE")
inline fun unsupported(): Nothing {
    throw UnsupportedOperationException()
}

internal fun getUnits(s: String): String {
    var i = s.length - 1
    while (i >= 0) {
        val c = s[i]
        if (!c.isLetter())
            break
        i -= 1
    }
    return s.substring(i + 1)
}

/**
 * Returns default value if string is empty, original string otherwise.
 */
fun String.notEmptyOr(default: String): String = if (isEmpty()) default else this

fun String.toLittleCase(): String {
    return if (this.all { it.isUpperCase() }) {
        this.toLowerCase()
    } else {
        when (val firstLowerCaseIndex = this.indexOfFirst { it.isLowerCase() }) {
            -1, 0 -> this
            1 -> this[0].toLowerCase() + this.drop(1)
            else ->
                this.substring(0, firstLowerCaseIndex - 1).toLowerCase() +
                    this.substring(firstLowerCaseIndex - 1)
        }
    }
}

/**
 * Modified implementation from [org.apache.commons.text.CaseUtils.toCamelCase].
 */
fun String.toCamelCase(): String {
    if (isEmpty()) {
        return this
    }
    val strLen = this.length
    val newCodePoints = IntArray(strLen)
    var outOffset = 0
    val delimiterSet = setOf(" ".codePointAt(0), "_".codePointAt(0))
    var capitalizeNext = Character.isUpperCase(this.codePointAt(0))
    var lowercaseNext = false
    var previousIsUppercase = false
    var index = 0
    while (index < strLen) {
        val codePoint: Int = this.codePointAt(index)
        when {
            delimiterSet.contains(codePoint) -> {
                capitalizeNext = outOffset != 0
                lowercaseNext = false
                previousIsUppercase = false
                index += Character.charCount(codePoint)
            }
            capitalizeNext -> {
                val upperCaseCodePoint = Character.toUpperCase(codePoint)
                newCodePoints[outOffset++] = upperCaseCodePoint
                index += Character.charCount(upperCaseCodePoint)
                capitalizeNext = false
                lowercaseNext = true
            }
            lowercaseNext -> {
                val lowerCaseCodePoint = Character.toLowerCase(codePoint)
                newCodePoints[outOffset++] = lowerCaseCodePoint
                index += Character.charCount(lowerCaseCodePoint)
                if (Character.isLowerCase(codePoint)) {
                    lowercaseNext = false
                    if (previousIsUppercase) {
                        previousIsUppercase = false
                        val previousCodePoint = newCodePoints[outOffset - 2]
                        val upperCaseCodePoint = Character.toUpperCase(previousCodePoint)
                        newCodePoints[outOffset - 2] = upperCaseCodePoint
                    }
                } else {
                    previousIsUppercase = true
                }
            }
            else -> {
                newCodePoints[outOffset++] = codePoint
                index += Character.charCount(codePoint)
            }
        }
    }
    return if (outOffset != 0) {
        String(newCodePoints, 0, outOffset)
    } else this
}

fun String.toLittleCamelCase(): String {
    return this.toCamelCase().toLittleCase()
}

fun tempDirectory(
    prefix: String = "tmp",
    suffix: String? = null,
    directory: File? = null
): File {
    return createTempDir(prefix, suffix, directory)
}

fun tempFile(prefix: String = "tmp", suffix: String? = null, directory: File? = null): File {
    return createTempFile(prefix, suffix, directory)
}
