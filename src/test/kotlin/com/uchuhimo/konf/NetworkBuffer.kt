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
