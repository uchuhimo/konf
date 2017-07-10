package com.uchuhimo.konf

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.type.TypeFactory

sealed class Item<T : Any>(
        val spec: ConfigSpec,
        name: String,
        val description: String = "") {
    init {
        spec.addItem(this)
    }

    val name: String = spec.qualify(name)

    val path: Path = run {
        val path = this.name.split('.')
        check("" !in path) { "${this.name} is invalid name for item" }
        path
    }

    val type: JavaType = TypeFactory.defaultInstance().constructType(this::class.java)
            .findSuperType(Item::class.java).bindings.typeParameters[0]

    open val isRequired: Boolean get() = false

    open val isOptional: Boolean get() = false

    open val isLazy: Boolean get() = false

    val asRequiredItem: RequiredItem<T> get() = this as RequiredItem<T>

    val asOptionalItem: OptionalItem<T> get() = this as OptionalItem<T>

    val asLazyItem: LazyItem<T> get() = this as LazyItem<T>
}

typealias Path = List<String>

val Path.name: String get() = joinToString(".")

fun String.toPath(): Path = listOf(this)

open class RequiredItem<T : Any>(
        spec: ConfigSpec,
        name: String,
        description: String = ""
) : Item<T>(spec, name, description) {
    override val isRequired: Boolean = true
}

open class OptionalItem<T : Any>(
        spec: ConfigSpec,
        name: String,
        val default: T,
        description: String = ""
) : Item<T>(spec, name, description) {
    override val isOptional: Boolean = true
}

open class LazyItem<T : Any>(
        spec: ConfigSpec,
        name: String,
        val thunk: (ConfigGetter) -> T,
        val placeholder: String = "",
        description: String = ""
) : Item<T>(spec, name, description) {
    override val isLazy: Boolean = true
}
