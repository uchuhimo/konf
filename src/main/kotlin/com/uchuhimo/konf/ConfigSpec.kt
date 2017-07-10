package com.uchuhimo.konf

open class ConfigSpec(val prefix: String = "") {
    private val _items = mutableListOf<Item<*>>()

    val items: List<Item<*>> = _items

    @Suppress("NOTHING_TO_INLINE")
    inline fun <T : Any> required(name: String, description: String = "") =
            object : RequiredItem<T>(
                    spec = this,
                    name = name,
                    description = description
            ) {}

    @Suppress("NOTHING_TO_INLINE")
    inline fun <T : Any> optional(name: String, default: T, description: String = "") =
            object : OptionalItem<T>(
                    spec = this,
                    name = name,
                    default = default,
                    description = description
            ) {}

    @Suppress("NOTHING_TO_INLINE")
    inline fun <T : Any> lazy(
            name: String,
            description: String = "",
            placeholder: String = "",
            noinline default: (ConfigGetter) -> T) =
            object : LazyItem<T>(
                    spec = this,
                    name = name,
                    thunk = default,
                    placeholder = placeholder,
                    description = description
            ) {}

    internal fun qualify(name: String) = if (prefix.isEmpty()) name else "$prefix.$name"

    internal fun addItem(item: Item<*>) {
        _items += item
    }
}
