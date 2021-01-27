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

class AdHocNetworkBuffer(config: Config) {
    private val root = config.at("network.buffer")

    val size: Int by root.required(description = "size of buffer in KB")

    val maxSize by root.lazy(name = "max-size", description = "max size of buffer in KB") { size * 2 }

    val name by root.optional("buffer", description = "name of buffer")

    val type by root.optional(
        Type.OFF_HEAP,
        prefix = "heap",
        description =
        """
            | type of network buffer.
            | two type:
            | - on-heap
            | - off-heap
            | buffer is off-heap by default.
            """.trimMargin("| ")
    )

    val offset by root.optional<Int?>(null, description = "initial offset of buffer")

    enum class Type {
        ON_HEAP, OFF_HEAP
    }
}
