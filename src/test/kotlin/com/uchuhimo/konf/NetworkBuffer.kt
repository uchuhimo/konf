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

class NetworkBuffer {
    companion object : ConfigSpec("network.buffer") {
        @JvmField
        val size = required<Int>(name = "size", description = "size of buffer in KB")

        @JvmField
        val maxSize = lazy(
                name = "maxSize",
                description = "max size of buffer in KB",
                placeholder = "${size.name} * 2"
        ) { it[size] * 2 }

        @JvmField
        val name = optional(
                name = "name",
                default = "buffer",
                description = "name of buffer")

        @JvmField
        val type = optional(
                name = "type",
                default = Type.OFF_HEAP,
                description = """
                              | type of network buffer.
                              | two type:
                              | - on-heap
                              | - off-heap
                              | buffer is off-heap by default.
                              """.trimMargin("| "))
    }

    enum class Type {
        ON_HEAP, OFF_HEAP
    }
}
