/*
 * Copyright 2017-2019 the original author or authors.
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

/**
 * Information of source for debugging.
 */
class SourceInfo(
    private val info: MutableMap<String, String> = mutableMapOf()
) : MutableMap<String, String> by info {
    /**
     * Description of this source's information.
     */
    val description
        get() = map { (name, value) ->
            "$name: $value"
        }.joinToString(separator = ", ", prefix = "[", postfix = "]")

    companion object {
        /**
         * Return a new source info with specified info.
         *
         * @param info info to be inherited
         * @return a new source info
         */
        fun with(info: Map<String, String>): SourceInfo {
            return SourceInfo(info.toMutableMap())
        }
    }
}
