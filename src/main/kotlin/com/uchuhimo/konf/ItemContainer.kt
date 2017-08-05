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

interface ItemContainer {
    operator fun <T : Any> get(item: Item<T>): T
    operator fun <T : Any> get(name: String): T
    fun <T : Any> getOrNull(item: Item<T>): T?
    fun <T : Any> getOrNull(name: String): T?
    operator fun <T : Any> invoke(name: String): T = get(name)
    operator fun iterator(): Iterator<Item<*>>
    operator fun contains(item: Item<*>): Boolean
    operator fun contains(name: String): Boolean
    val items: List<Item<*>> get() = mutableListOf<Item<*>>().apply {
        addAll(this@ItemContainer.iterator().asSequence())
    }
}
