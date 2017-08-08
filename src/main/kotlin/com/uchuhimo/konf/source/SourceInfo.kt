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

/**
 * Information of source for debugging.
 */
interface SourceInfo {
    /**
     * Description of this source.
     */
    val description: String get() = (info + context).toDescription()

    /**
     * Information about context of this source.
     *
     * Context is in form of key-value pairs.
     * Context can be inherited by other related source.
     */
    val context: Map<String, String>

    /**
     * Add information into context.
     */
    fun addContext(name: String, value: String)

    /**
     * Information about this source.
     *
     * Info is in form of key-value pairs.
     */
    val info: Map<String, String>

    /**
     * Add information into info.
     */
    fun addInfo(name: String, value: String)

    companion object {
        /**
         * Return a new empty source info with specified context.
         *
         * @param context context to be inherited
         * @return a new empty source info
         */
        fun with(context: Map<String, String>): SourceInfo = object : SourceInfo {
            private val _info = mutableMapOf<String, String>()

            override val info: Map<String, String> get() = _info

            override fun addInfo(name: String, value: String) {
                _info.put(name, value)
            }

            private val _context: MutableMap<String, String> = context.toMutableMap()

            override val context: Map<String, String> get() = _context

            override fun addContext(name: String, value: String) {
                _context.put(name, value)
            }
        }

        /**
         * Returns a new empty source info.
         *
         * @return a new empty source info
         */
        fun default(): SourceInfo = with(mapOf())
    }
}
