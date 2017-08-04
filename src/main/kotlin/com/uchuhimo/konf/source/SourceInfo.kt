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

interface SourceInfo {
    val description: String get() = (info + context).toDescription()

    val context: Map<String, String>

    fun addContext(name: String, value: String)

    val info: Map<String, String>

    fun addInfo(name: String, value: String)

    companion object {
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

        fun default(): SourceInfo = with(mapOf())
    }
}
