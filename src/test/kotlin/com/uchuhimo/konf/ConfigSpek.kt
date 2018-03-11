/*
 * Copyright 2017-2018 the original author or authors.
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

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.isEmpty
import com.natpryce.hamkrest.sameInstance
import com.natpryce.hamkrest.throws
import com.uchuhimo.konf.source.base.asKVSource
import com.uchuhimo.konf.source.base.toHierarchicalMap
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek
import org.jetbrains.spek.subject.dsl.SubjectProviderDsl
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

object ConfigSpek : SubjectSpek<Config>({
    subject { Config { addSpec(NetworkBuffer) } }

    configSpek()
})

fun SubjectProviderDsl<Config>.configSpek(prefix: String = "network.buffer") {
    val spec = NetworkBuffer
    val size = NetworkBuffer.size
    val maxSize = NetworkBuffer.maxSize
    val name = NetworkBuffer.name
    val type = NetworkBuffer.type

    fun qualify(name: String): String = if (prefix.isEmpty()) name else "$prefix.$name"

    given("a config") {
        val invalidItem by ConfigSpec("invalid").run { required<Int>() }
        val invalidItemName = "invalid.invalidItem"
        group("addSpec operation") {
            on("add orthogonal spec") {
                val newSpec = object : ConfigSpec(spec.prefix) {
                    val minSize by optional(1)
                }
                subject.addSource(mapOf(newSpec.qualify(newSpec.minSize.name) to 2).asKVSource())
                subject.addSpec(newSpec)
                it("should contain items in new spec") {
                    assertThat(newSpec.minSize in subject, equalTo(true))
                    assertThat(spec.qualify(newSpec.minSize.name) in subject, equalTo(true))
                }
                it("should contain new spec") {
                    assertThat(newSpec in subject.specs, equalTo(true))
                    assertThat(spec in subject.specs, equalTo(true))
                }
                it("should load values from the existed sources for items in new spec") {
                    assertThat(subject[newSpec.minSize], equalTo(2))
                }
            }
            on("add repeated item") {
                it("should throw RepeatedItemException") {
                    assertThat({ subject.addSpec(spec) }, throws(has(
                        RepeatedItemException::name,
                        equalTo(spec.qualify(size.name)))))
                }
            }
            on("add repeated name") {
                val newSpec = ConfigSpec(prefix).apply {
                    @Suppress("UNUSED_VARIABLE", "NAME_SHADOWING")
                    val size by required<Int>()
                }
                it("should throw NameConflictException") {
                    assertThat({ subject.addSpec(newSpec) }, throws<NameConflictException>())
                }
            }
            on("add conflict name, which is prefix of existed name") {
                val newSpec = ConfigSpec().apply {
                    @Suppress("UNUSED_VARIABLE")
                    val buffer by required<Int>()
                }
                it("should throw NameConflictException") {
                    assertThat({
                        subject.addSpec(
                            newSpec.withPrefix(prefix.toPath().let { it.subList(0, it.size - 1) }.name))
                    }, throws<NameConflictException>())
                }
            }
            on("add conflict name, and an existed name is prefix of it") {
                val newSpec = ConfigSpec(qualify(type.name)).apply {
                    @Suppress("UNUSED_VARIABLE")
                    val subType by required<Int>()
                }
                it("should throw NameConflictException") {
                    assertThat({ subject.addSpec(newSpec) }, throws<NameConflictException>())
                }
            }
        }
        on("iterate items in config") {
            it("should cover all items in config") {
                assertThat(subject.items.toSet(), equalTo(spec.items.toSet()))
            }
        }
        on("iterate name of items in config") {
            it("should cover all items in config") {
                assertThat(subject.nameOfItems.toSet(), equalTo(spec.items.map { qualify(it.name) }.toSet()))
            }
        }
        on("export values to map") {
            it("should not contain unset items in map") {
                assertThat(subject.toMap(), equalTo(mapOf<String, Any>(
                    qualify(spec.name.name) to "buffer",
                    qualify(spec.type.name) to NetworkBuffer.Type.OFF_HEAP.name)))
            }
            it("should contain corresponding items in map") {
                subject[spec.size] = 4
                subject[spec.type] = NetworkBuffer.Type.ON_HEAP
                val map = subject.toMap()
                assertThat(map, equalTo(mapOf(
                    qualify(spec.size.name) to 4,
                    qualify(spec.maxSize.name) to 8,
                    qualify(spec.name.name) to "buffer",
                    qualify(spec.type.name) to NetworkBuffer.Type.ON_HEAP.name)))
            }
            it("should recover all items when reloaded from map") {
                subject[spec.size] = 4
                subject[spec.type] = NetworkBuffer.Type.ON_HEAP
                val map = subject.toMap()
                val newConfig = Config { addSpec(spec[spec.prefix].withPrefix(prefix)) }.from.map.kv(map)
                assertThat(newConfig[spec.size], equalTo(4))
                assertThat(newConfig[spec.maxSize], equalTo(8))
                assertThat(newConfig[spec.name], equalTo("buffer"))
                assertThat(newConfig[spec.type], equalTo(NetworkBuffer.Type.ON_HEAP))
                assertThat(newConfig, equalTo(subject))
            }
        }
        on("export values to hierarchical map") {
            fun prefixToMap(prefix: String, value: Map<String, Any>): Map<String, Any> {
                return if (prefix.isEmpty()) {
                    value
                } else if (prefix.contains('.')) {
                    mapOf<String, Any>(
                        prefix.substring(0, prefix.indexOf('.')) to
                            prefixToMap(prefix.substring(prefix.indexOf('.') + 1), value))
                } else {
                    mapOf(prefix to value)
                }
            }
            it("should not contain unset items in map") {
                assertThat(subject.toHierarchicalMap(),
                    equalTo(prefixToMap(prefix, mapOf(
                        "name" to "buffer",
                        "type" to NetworkBuffer.Type.OFF_HEAP.name))))
            }
            it("should contain corresponding items in map") {
                subject[spec.size] = 4
                subject[spec.type] = NetworkBuffer.Type.ON_HEAP
                val map = subject.toHierarchicalMap()
                assertThat(map,
                    equalTo(prefixToMap(prefix, mapOf(
                        "size" to 4,
                        "maxSize" to 8,
                        "name" to "buffer",
                        "type" to NetworkBuffer.Type.ON_HEAP.name))))
            }
            it("should recover all items when reloaded from map") {
                subject[spec.size] = 4
                subject[spec.type] = NetworkBuffer.Type.ON_HEAP
                val map = subject.toHierarchicalMap()
                val newConfig = Config { addSpec(spec[spec.prefix].withPrefix(prefix)) }.from.map.hierarchical(map)
                assertThat(newConfig[spec.size], equalTo(4))
                assertThat(newConfig[spec.maxSize], equalTo(8))
                assertThat(newConfig[spec.name], equalTo("buffer"))
                assertThat(newConfig[spec.type], equalTo(NetworkBuffer.Type.ON_HEAP))
                assertThat(newConfig, equalTo(subject))
            }
        }
        on("object methods") {
            val map = mapOf(
                qualify(spec.name.name) to "buffer",
                qualify(spec.type.name) to NetworkBuffer.Type.OFF_HEAP.name)
            it("should not equal to object of other class") {
                assertFalse(subject.equals(1))
            }
            it("should equal to itself") {
                assertThat(subject, equalTo(subject))
            }
            it("should have same hash code with map with same content") {
                assertThat(subject.hashCode(), equalTo(map.hashCode()))
            }
            it("should convert to string in map-like format") {
                assertThat(subject.toString(), equalTo("Config(items=$map)"))
            }
        }
        group("get operation") {
            on("get with valid item") {
                it("should return corresponding value") {
                    assertThat(subject[name], equalTo("buffer"))
                }
            }
            on("get with invalid item") {
                it("should throw NoSuchItemException when using `get`") {
                    assertThat({ subject[invalidItem] },
                        throws(has(NoSuchItemException::name, equalTo(invalidItem.asName))))
                }
                it("should return null when using `getOrNull`") {
                    assertThat(subject.getOrNull(invalidItem), absent())
                }
            }
            on("get with valid name") {
                it("should return corresponding value") {
                    assertThat(subject(qualify("name")), equalTo("buffer"))
                }
            }
            on("get with invalid name") {
                it("should throw NoSuchItemException when using `get`") {
                    assertThat({ subject<String>(spec.qualify("invalid")) }, throws(has(
                        NoSuchItemException::name, equalTo(spec.qualify("invalid")))))
                }
                it("should return null when using `getOrNull`") {
                    assertThat(subject.getOrNull<String>(spec.qualify("invalid")), absent())
                }
            }
            on("get unset item") {
                it("should throw UnsetValueException") {
                    assertThat({ subject[size] }, throws(has(
                        UnsetValueException::name,
                        equalTo(size.asName))))
                    assertThat({ subject[maxSize] }, throws(has(
                        UnsetValueException::name,
                        equalTo(size.asName))))
                }
            }
        }
        group("set operation") {
            on("set with valid item when corresponding value is unset") {
                subject[size] = 1024
                it("should contain the specified value") {
                    assertThat(subject[size], equalTo(1024))
                }
            }
            on("set with valid item when corresponding value exists") {
                subject[name] = "newName"
                it("should contain the specified value") {
                    assertThat(subject[name], equalTo("newName"))
                }
            }
            on("set with valid item when corresponding value is lazy") {
                test("before set, the item should be lazy; after set," +
                    " the item should be no longer lazy, and it contains the specified value") {
                    subject[size] = 1024
                    assertThat(subject[maxSize], equalTo(subject[size] * 2))
                    subject[maxSize] = 0
                    assertThat(subject[maxSize], equalTo(0))
                    subject[size] = 2048
                    assertThat(subject[maxSize], !equalTo(subject[size] * 2))
                    assertThat(subject[maxSize], equalTo(0))
                }
            }
            on("set with invalid item") {
                it("should throw NoSuchItemException") {
                    assertThat({ subject[invalidItem] = 1024 },
                        throws(has(NoSuchItemException::name, equalTo(invalidItem.asName))))
                }
            }
            on("set with valid name") {
                subject[qualify("size")] = 1024
                it("should contain the specified value") {
                    assertThat(subject[size], equalTo(1024))
                }
            }
            on("set with invalid name") {
                it("should throw NoSuchItemException") {
                    assertThat({ subject[invalidItemName] = 1024 },
                        throws(has(NoSuchItemException::name, equalTo(invalidItemName))))
                }
            }
            on("set with incorrect type of value") {
                it("should throw ClassCastException") {
                    assertThat({ subject[qualify(size.name)] = "1024" }, throws<ClassCastException>())
                }
            }
            on("lazy set with valid item") {
                subject.lazySet(maxSize) { it[size] * 4 }
                subject[size] = 1024
                it("should contain the specified value") {
                    assertThat(subject[maxSize], equalTo(subject[size] * 4))
                }
            }
            on("lazy set with invalid item") {
                it("should throw NoSuchItemException") {
                    assertThat({ subject.lazySet(invalidItem) { 1024 } },
                        throws(has(NoSuchItemException::name, equalTo(invalidItem.asName))))
                }
            }
            on("lazy set with valid name") {
                subject.lazySet(qualify(maxSize.name)) { it[size] * 4 }
                subject[size] = 1024
                it("should contain the specified value") {
                    assertThat(subject[maxSize], equalTo(subject[size] * 4))
                }
            }
            on("lazy set with valid name and invalid value with incompatible type") {
                subject.lazySet(qualify(maxSize.name)) { "string" }
                it("should throw InvalidLazySetException when getting") {
                    assertThat({ subject[qualify(maxSize.name)] }, throws<InvalidLazySetException>())
                }
            }
            on("lazy set with invalid name") {
                it("should throw NoSuchItemException") {
                    assertThat({ subject.lazySet(invalidItemName) { 1024 } },
                        throws(has(NoSuchItemException::name, equalTo(invalidItemName))))
                }
            }
            on("unset with valid item") {
                subject.unset(type)
                it("should contain `null` when using `getOrNull`") {
                    assertThat(subject.getOrNull(type), absent())
                }
            }
            on("unset with invalid item") {
                it("should throw NoSuchItemException") {
                    assertThat({ subject.unset(invalidItem) },
                        throws(has(NoSuchItemException::name, equalTo(invalidItem.asName))))
                }
            }
            on("unset with valid name") {
                subject.unset(qualify(type.name))
                it("should contain `null` when using `getOrNull`") {
                    assertThat(subject.getOrNull(type), absent())
                }
            }
            on("unset with invalid name") {
                it("should throw NoSuchItemException") {
                    assertThat({ subject.unset(invalidItemName) },
                        throws(has(NoSuchItemException::name, equalTo(invalidItemName))))
                }
            }
        }
        group("item property") {
            on("declare a property by item") {
                var nameProperty by subject.property(name)
                it("should behave same as `get`") {
                    assertThat(nameProperty, equalTo(subject[name]))
                }
                it("should support set operation as `set`") {
                    nameProperty = "newName"
                    assertThat(nameProperty, equalTo("newName"))
                }
            }
            on("declare a property by invalid item") {
                it("should throw NoSuchItemException") {
                    assertThat({
                        @Suppress("UNUSED_VARIABLE")
                        var nameProperty by subject.property(invalidItem)
                    }, throws(has(NoSuchItemException::name, equalTo(invalidItem.asName))))
                }
            }
            on("declare a property by name") {
                var nameProperty by subject.property<String>(qualify(name.name))
                it("should behave same as `get`") {
                    assertThat(nameProperty, equalTo(subject[name]))
                }
                it("should support set operation as `set`") {
                    nameProperty = "newName"
                    assertThat(nameProperty, equalTo("newName"))
                }
            }
            on("declare a property by invalid name") {
                it("should throw NoSuchItemException") {
                    assertThat({
                        @Suppress("UNUSED_VARIABLE")
                        var nameProperty by subject.property<Int>(invalidItemName)
                    }, throws(has(NoSuchItemException::name, equalTo(invalidItemName))))
                }
            }
        }
        group("layer") {
            val layer by memoized {
                subject.withLayer("layer").apply {
                    this[size] = 4
                    this.unset(maxSize)
                }.layer
            }
            it("should have not parent") {
                assertNull(layer.parent)
            }
            it("should contain specs in this layer") {
                assertThat(layer.specs, isEmpty)
            }
            it("should return itself as its layer") {
                assertThat(layer.layer, sameInstance(layer))
            }
            on("iterate items in layer") {
                it("should cover all items in layer") {
                    assertThat(layer.items.toSet(), equalTo(setOf<Item<*>>(size, maxSize)))
                }
            }
            on("export values to map") {
                it("should contain corresponding items in map") {
                    val map = layer.toMap()
                    assertThat(map, equalTo(mapOf<String, Any>(qualify(size.name) to 4)))
                }
            }
            on("object methods") {
                val map = mapOf(qualify(spec.size.name) to 4)
                it("should not equal to object of other class") {
                    assertFalse(layer.equals(1))
                }
                it("should not equal to config with different items") {
                    assertTrue(layer != subject)
                }
                it("should equal to itself") {
                    assertThat(layer, equalTo(layer))
                }
                it("should have same hash code with map with same content") {
                    assertThat(layer.hashCode(), equalTo(map.hashCode()))
                }
                it("should convert to string in map-like format") {
                    assertThat(layer.toString(), equalTo("Layer(items=$map)"))
                }
            }
            group("get operation") {
                on("get with valid item") {
                    it("should return corresponding value") {
                        assertThat(layer[size], equalTo(4))
                        assertThat(layer.getOrNull(size), equalTo(4))
                        assertTrue(size in layer)
                    }
                }
                on("get with invalid item") {
                    it("should throw NoSuchItemException when using `get`") {
                        assertThat({ layer[name] },
                            throws(has(NoSuchItemException::name, equalTo(name.asName))))
                    }
                    it("should return null when using `getOrNull`") {
                        assertThat(layer.getOrNull(name), absent())
                        assertFalse(name in layer)
                    }
                }
                on("get with valid name") {
                    it("should return corresponding value") {
                        assertThat(layer(qualify("size")), equalTo(4))
                        assertThat(layer.getOrNull(qualify("size")), equalTo(4))
                        assertTrue(qualify("size") in layer)
                    }
                }
                on("get with invalid name") {
                    it("should throw NoSuchItemException when using `get`") {
                        assertThat({ layer<Int>(spec.qualify("name")) }, throws(has(
                            NoSuchItemException::name, equalTo(spec.qualify("name")))))
                    }
                    it("should return null when using `getOrNull`") {
                        assertThat(layer.getOrNull<Int>(spec.qualify("name")), absent())
                        assertFalse(spec.qualify("name") in layer)
                    }
                }
                on("get unset item") {
                    it("should throw UnsetValueException") {
                        assertThat({ layer[maxSize] }, throws(has(
                            UnsetValueException::name,
                            equalTo(maxSize.asName))))
                    }
                }
            }
        }
    }
}
