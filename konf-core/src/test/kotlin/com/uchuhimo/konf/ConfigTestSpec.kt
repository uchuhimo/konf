/*
 * Copyright 2017-2019 the original author or authors.
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

object ConfigTestSpec : SubjectSpek<Config>({
    subject { Config { addSpec(NetworkBuffer) } }

    configTestSpec()
})

fun SubjectProviderDsl<Config>.configTestSpec(prefix: String = "network.buffer") {
    val spec = NetworkBuffer
    val size = NetworkBuffer.size
    val maxSize = NetworkBuffer.maxSize
    val name = NetworkBuffer.name
    val type = NetworkBuffer.type
    val offset = NetworkBuffer.offset

    fun qualify(name: String): String = if (prefix.isEmpty()) name else "$prefix.$name"

    given("a config") {
        val invalidItem by ConfigSpec("invalid").run { required<Int>() }
        val invalidItemName = "invalid.invalidItem"
        group("feature operation") {
            on("enable feature") {
                subject.enable(Feature.FAIL_ON_UNKNOWN_PATH)
                it("should let the feature be enabled") {
                    assertTrue { subject.isEnabled(Feature.FAIL_ON_UNKNOWN_PATH) }
                }
            }
            on("disable feature") {
                subject.disable(Feature.FAIL_ON_UNKNOWN_PATH)
                it("should let the feature be disabled") {
                    assertFalse { subject.isEnabled(Feature.FAIL_ON_UNKNOWN_PATH) }
                }
            }
            on("by default") {
                it("should use the feature's default setting") {
                    assertThat(
                        subject.isEnabled(Feature.FAIL_ON_UNKNOWN_PATH),
                        equalTo(Feature.FAIL_ON_UNKNOWN_PATH.enabledByDefault))
                }
            }
        }
        group("addSpec operation") {
            on("add orthogonal spec") {
                val newSpec = object : ConfigSpec(spec.prefix) {
                    val minSize by optional(1)
                }
                val config = subject.withSource(mapOf(newSpec.qualify(newSpec.minSize) to 2).asKVSource())
                config.addSpec(newSpec)
                it("should contain items in new spec") {
                    assertTrue { newSpec.minSize in config }
                    assertTrue { spec.qualify(newSpec.minSize) in config }
                    assertThat(config.nameOf(newSpec.minSize), equalTo(spec.qualify(newSpec.minSize)))
                }
                it("should contain new spec") {
                    assertThat(newSpec in config.specs, equalTo(true))
                    assertThat(spec in config.specs, equalTo(true))
                }
                it("should load values from the existed sources for items in new spec") {
                    assertThat(config[newSpec.minSize], equalTo(2))
                }
            }
            on("add spec with inner specs") {
                subject.addSpec(Service)
                it("should contain items in new spec") {
                    assertTrue { Service.name in subject }
                    assertTrue { Service.UI.host in subject }
                    assertTrue { Service.UI.port in subject }
                    assertTrue { Service.Backend.host in subject }
                    assertTrue { Service.Backend.port in subject }
                    assertTrue { Service.Backend.Login.user in subject }
                    assertTrue { Service.Backend.Login.password in subject }
                    assertTrue { "service.name" in subject }
                    assertTrue { "service.ui.host" in subject }
                    assertTrue { "service.ui.port" in subject }
                    assertTrue { "service.backend.host" in subject }
                    assertTrue { "service.backend.port" in subject }
                    assertTrue { "service.backend.login.user" in subject }
                    assertTrue { "service.backend.login.password" in subject }
                }
                it("should contain new spec") {
                    assertTrue { Service in subject.specs }
                }
                it("should not contain inner specs in new spec") {
                    assertFalse { Service.UI in subject.specs }
                    assertFalse { Service.Backend in subject.specs }
                    assertFalse { Service.Backend.Login in subject.specs }
                }
            }
            on("add nested spec") {
                subject.addSpec(Service.Backend)
                it("should contain items in the nested spec") {
                    assertTrue { Service.Backend.host in subject }
                    assertTrue { Service.Backend.port in subject }
                    assertTrue { Service.Backend.Login.user in subject }
                    assertTrue { Service.Backend.Login.password in subject }
                }
                it("should not contain items in the outer spec") {
                    assertFalse { Service.name in subject }
                    assertFalse { Service.UI.host in subject }
                    assertFalse { Service.UI.port in subject }
                }
                it("should contain the nested spec") {
                    assertTrue { Service.Backend in subject.specs }
                }
                it("should not contain the outer spec or inner specs in the nested spec") {
                    assertFalse { Service in subject.specs }
                    assertFalse { Service.UI in subject.specs }
                    assertFalse { Service.Backend.Login in subject.specs }
                }
            }
            on("add repeated item") {
                it("should throw RepeatedItemException") {
                    assertThat({ subject.addSpec(spec) }, throws(has(
                        RepeatedItemException::name,
                        equalTo(spec.qualify(size)))))
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
        group("addItem operation") {
            on("add orthogonal item") {
                val minSize by Spec.dummy.optional(1)
                val config = subject.withSource(mapOf(spec.qualify(minSize) to 2).asKVSource())
                config.addItem(minSize, spec.prefix)
                it("should contain item") {
                    assertTrue { minSize in config }
                    assertTrue { spec.qualify(minSize) in config }
                    assertThat(config.nameOf(minSize), equalTo(spec.qualify(minSize)))
                }
                it("should load values from the existed sources for item") {
                    assertThat(config[minSize], equalTo(2))
                }
            }
            on("add repeated item") {
                it("should throw RepeatedItemException") {
                    assertThat({ subject.addItem(size, spec.prefix) }, throws(has(
                        RepeatedItemException::name,
                        equalTo(spec.qualify(size)))))
                }
            }
            on("add repeated name") {
                @Suppress("NAME_SHADOWING")
                val size by Spec.dummy.required<Int>()
                it("should throw NameConflictException") {
                    assertThat({ subject.addItem(size, prefix) }, throws<NameConflictException>())
                }
            }
            on("add conflict name, which is prefix of existed name") {
                val buffer by Spec.dummy.required<Int>()
                it("should throw NameConflictException") {
                    assertThat({
                        subject.addItem(
                            buffer,
                            prefix.toPath().let { it.subList(0, it.size - 1) }.name)
                    }, throws<NameConflictException>())
                }
            }
            on("add conflict name, and an existed name is prefix of it") {
                val subType by Spec.dummy.required<Int>()
                it("should throw NameConflictException") {
                    assertThat({ subject.addItem(subType, qualify(type.name)) }, throws<NameConflictException>())
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
                    qualify(name.name) to "buffer",
                    qualify(type.name) to NetworkBuffer.Type.OFF_HEAP.name,
                    qualify(offset.name) to "null")))
            }
            it("should contain corresponding items in map") {
                subject[size] = 4
                subject[type] = NetworkBuffer.Type.ON_HEAP
                subject[offset] = 0
                val map = subject.toMap()
                assertThat(map, equalTo(mapOf(
                    qualify(size.name) to 4,
                    qualify(maxSize.name) to 8,
                    qualify(name.name) to "buffer",
                    qualify(type.name) to NetworkBuffer.Type.ON_HEAP.name,
                    qualify(offset.name) to 0)))
            }
            it("should recover all items when reloaded from map") {
                subject[size] = 4
                subject[type] = NetworkBuffer.Type.ON_HEAP
                subject[offset] = 0
                val map = subject.toMap()
                val newConfig = Config { addSpec(spec[spec.prefix].withPrefix(prefix)) }.from.map.kv(map)
                assertThat(newConfig[size], equalTo(4))
                assertThat(newConfig[maxSize], equalTo(8))
                assertThat(newConfig[name], equalTo("buffer"))
                assertThat(newConfig[type], equalTo(NetworkBuffer.Type.ON_HEAP))
                assertThat(newConfig[offset], equalTo(0))
                assertThat(newConfig.toMap(), equalTo(subject.toMap()))
            }
        }
        on("export values to hierarchical map") {
            fun prefixToMap(prefix: String, value: Map<String, Any>): Map<String, Any> {
                return when {
                    prefix.isEmpty() -> value
                    prefix.contains('.') ->
                        mapOf<String, Any>(
                            prefix.substring(0, prefix.indexOf('.')) to
                                prefixToMap(prefix.substring(prefix.indexOf('.') + 1), value))
                    else -> mapOf(prefix to value)
                }
            }
            it("should not contain unset items in map") {
                assertThat(subject.toHierarchicalMap(),
                    equalTo(prefixToMap(prefix, mapOf(
                        "name" to "buffer",
                        "type" to NetworkBuffer.Type.OFF_HEAP.name,
                        "offset" to "null"
                    ))))
            }
            it("should contain corresponding items in map") {
                subject[size] = 4
                subject[type] = NetworkBuffer.Type.ON_HEAP
                subject[offset] = 0
                val map = subject.toHierarchicalMap()
                assertThat(map,
                    equalTo(prefixToMap(prefix, mapOf(
                        "size" to 4,
                        "maxSize" to 8,
                        "name" to "buffer",
                        "type" to NetworkBuffer.Type.ON_HEAP.name,
                        "offset" to 0
                    ))))
            }
            it("should recover all items when reloaded from map") {
                subject[size] = 4
                subject[type] = NetworkBuffer.Type.ON_HEAP
                subject[offset] = 0
                val map = subject.toHierarchicalMap()
                val newConfig = Config { addSpec(spec[spec.prefix].withPrefix(prefix)) }.from.map.hierarchical(map)
                assertThat(newConfig[size], equalTo(4))
                assertThat(newConfig[maxSize], equalTo(8))
                assertThat(newConfig[name], equalTo("buffer"))
                assertThat(newConfig[type], equalTo(NetworkBuffer.Type.ON_HEAP))
                assertThat(newConfig[offset], equalTo(0))
                assertThat(newConfig.toMap(), equalTo(subject.toMap()))
            }
        }
        on("object methods") {
            val map = mapOf(
                qualify(name.name) to "buffer",
                qualify(type.name) to NetworkBuffer.Type.OFF_HEAP.name,
                qualify(offset.name) to "null"
            )
            it("should not equal to object of other class") {
                assertFalse(subject.equals(1))
            }
            it("should equal to itself") {
                assertThat(subject, equalTo(subject))
            }
            it("should convert to string in map-like format") {
                assertThat(subject.toString(), equalTo("Config(items=$map)"))
            }
        }
        on("lock config") {
            it("should be locked") {
                subject.lock { }
            }
        }
        group("get operation") {
            on("get with valid item") {
                it("should return corresponding value") {
                    assertThat(subject[name], equalTo("buffer"))
                    assertTrue { name in subject }
                    assertNull(subject[offset])
                    assertTrue { offset in subject }
                    assertNull(subject.getOrNull(maxSize))
                    assertTrue { maxSize in subject }
                }
            }
            on("get with invalid item") {
                it("should throw NoSuchItemException when using `get`") {
                    assertThat({ subject[invalidItem] },
                        throws(has(NoSuchItemException::name, equalTo(invalidItem.asName))))
                }
                it("should return null when using `getOrNull`") {
                    assertThat(subject.getOrNull(invalidItem), absent())
                    assertTrue { invalidItem !in subject }
                }
            }
            on("get with valid name") {
                it("should return corresponding value") {
                    assertThat(subject(qualify("name")), equalTo("buffer"))
                    assertThat(subject.getOrNull(qualify("name")), equalTo("buffer"))
                    assertTrue { qualify("name") in subject }
                }
            }
            on("get with valid name which contains trailing whitespaces") {
                it("should return corresponding value") {
                    assertThat(subject(qualify("name ")), equalTo("buffer"))
                    assertThat(subject.getOrNull(qualify("name  ")), equalTo("buffer"))
                    assertTrue { qualify("name   ") in subject }
                }
            }
            on("get with invalid name") {
                it("should throw NoSuchItemException when using `get`") {
                    assertThat({ subject<String>(spec.qualify(invalidItem)) }, throws(has(
                        NoSuchItemException::name, equalTo(spec.qualify(invalidItem)))))
                }
                it("should return null when using `getOrNull`") {
                    assertThat(subject.getOrNull<String>(spec.qualify(invalidItem)), absent())
                    assertTrue { spec.qualify(invalidItem) !in subject }
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
                    assertTrue { size in subject }
                    assertTrue { maxSize in subject }
                }
            }
            on("get with lazy item that returns null when the type is nullable") {
                it("should return null") {
                    val lazyItem by Spec.dummy.lazy<Int?> { null }
                    subject.addItem(lazyItem, prefix)
                    assertNull(subject[lazyItem])
                }
            }
            on("get with lazy item that returns null when the type is not nullable") {
                it("should throw InvalidLazySetException") {
                    @Suppress("UNCHECKED_CAST")
                    val thunk = { _: ItemContainer -> null } as (ItemContainer) -> Int
                    val lazyItem by Spec.dummy.lazy(thunk = thunk)
                    subject.addItem(lazyItem, prefix)
                    assertThat({ subject[lazyItem] }, throws<InvalidLazySetException>())
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
                it("should contain the specified value") {
                    subject[name] = "newName"
                    assertThat(subject[name], equalTo("newName"))
                    subject[offset] = 0
                    assertThat(subject[offset], equalTo(0))
                    subject[offset] = null
                    assertNull(subject[offset])
                }
            }
            on("raw set with valid item") {
                it("should contain the specified value") {
                    subject.rawSet(size, 2048)
                    assertThat(subject[size], equalTo(2048))
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
            on("set with valid name which contains trailing whitespaces") {
                subject[qualify("size  ")] = 1024
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
                    assertThat({ subject[qualify(size.name)] = null }, throws<ClassCastException>())
                }
            }
            on("set when onSet subscriber is defined") {
                var counter = 0
                size.onSet { counter += 1 }.use {
                    subject[size] = 1
                    subject[size] = 16
                    subject[size] = 256
                    subject[size] = 1024
                    it("should notify subscriber") {
                        assertThat(counter, equalTo(4))
                    }
                }
            }
            on("set when multiple onSet subscribers are defined") {
                var counter = 0
                size.onSet { counter += 1 }.use {
                    size.onSet { counter += 2 }.use {
                        subject[size] = 1
                        subject[size] = 16
                        subject[size] = 256
                        subject[size] = 1024
                        it("should notify all subscribers") {
                            assertThat(counter, equalTo(12))
                        }
                    }
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
            on("lazy set with valid name which contains trailing whitespaces") {
                subject.lazySet(qualify(maxSize.name + "  ")) { it[size] * 4 }
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
        on("clear operation") {
            it("should contain no value") {
                val config = if (subject.name == "multi-layer") {
                    subject.parent!!
                } else {
                    subject
                }
                assertTrue { name in config && type in config }
                config.clear()
                assertTrue { name !in config && type !in config }
            }
        }
        on("check whether all required items have values or not") {
            it("should return false when some required items don't have values") {
                assertFalse { subject.containsRequired() }
            }
            it("should return true when all required items have values") {
                subject[size] = 1
                assertTrue { subject.containsRequired() }
            }
        }
        on("validate whether all required items have values or not") {
            it("should throw UnsetValueException when some required items don't have values") {
                assertThat({
                    subject.validateRequired()
                }, throws<UnsetValueException>())
            }
            it("should return itself when all required items have values") {
                subject[size] = 1
                assertThat(subject, sameInstance(subject.validateRequired()))
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
    }
}

object Service : ConfigSpec() {
    val name by optional("test")

    object Backend : ConfigSpec() {
        val host by optional("127.0.0.1")
        val port by optional(7777)

        object Login : ConfigSpec() {
            val user by optional("admin")
            val password by optional("123456")
        }
    }

    object UI : ConfigSpec() {
        val host by optional("127.0.0.1")
        val port by optional(8888)
    }
}
