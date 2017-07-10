package com.uchuhimo.konf

sealed class ConfigTree {
    abstract val path: List<String>

    abstract fun deepCopy(): ConfigTree

    abstract val isItem: Boolean

    fun visit(
            onEnterPath: (ConfigPathNode) -> Unit = { _ -> },
            onLeavePath: (ConfigPathNode) -> Unit = { _ -> },
            onEnterItem: (ConfigItemNode<*>) -> Unit = { _ -> }) {
        when (this) {
            is ConfigPathNode -> {
                onEnterPath(this)
                for (child in children) {
                    child.visit(onEnterPath, onLeavePath, onEnterItem)
                }
                onLeavePath(this)
            }
            is ConfigItemNode<*> -> {
                onEnterItem(this)
            }
        }
    }

    val items: Iterable<Item<*>> get() {
        val items = mutableListOf<Item<*>>()
        visit(
                onEnterItem = { leaf ->
                    items += leaf.item
                })
        return items
    }
}

class ConfigItemNode<T : Any>(
        override val path: List<String>,
        val item: Item<T>
) : ConfigTree() {
    override fun deepCopy(): ConfigItemNode<T> = ConfigItemNode(path, item)

    override val isItem: Boolean = false
}

class ConfigPathNode(
        override val path: List<String>,
        val children: MutableList<ConfigTree>
) : ConfigTree() {
    override fun deepCopy(): ConfigPathNode =
            ConfigPathNode(path, children.mapTo(mutableListOf(), ConfigTree::deepCopy))

    override val isItem: Boolean = true
}
