/*
 * Copyright 2017-2018 the original author or authors.
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
