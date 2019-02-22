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

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.sameInstance
import com.natpryce.hamkrest.throws
import com.uchuhimo.konf.source.LoadException
import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.base.asKVSource
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek
import org.jetbrains.spek.subject.itBehavesLike
import java.util.ArrayDeque

object MultiLayerConfigSpec : SubjectSpek<Config>({

    subject { Config { addSpec(NetworkBuffer) }.withLayer("multi-layer") }

    itBehavesLike(ConfigSpek)

    group("multi-layer config") {
        it("should have specified name") {
            assertThat(subject.name, equalTo("multi-layer"))
        }
        it("should contain same items with parent config") {
            assertThat(subject[NetworkBuffer.name],
                equalTo(subject.parent!![NetworkBuffer.name]))
            assertThat(subject[NetworkBuffer.type],
                equalTo(subject.parent!![NetworkBuffer.type]))
            assertThat(subject[NetworkBuffer.offset],
                equalTo(subject.parent!![NetworkBuffer.offset]))
        }
        on("export values in facade layer to map") {
            val spec = NetworkBuffer
            it("should not contain unset items in map") {
                assertThat(subject.parent!!.layer.toMap(), equalTo(mapOf<String, Any>(
                    subject.nameOf(spec.name) to "buffer",
                    subject.nameOf(spec.type) to NetworkBuffer.Type.OFF_HEAP.name,
                    subject.nameOf(spec.offset) to "null")))
            }
            it("should not contain values from other layers in map") {
                subject[spec.size] = 4
                subject[spec.type] = NetworkBuffer.Type.ON_HEAP
                subject[spec.offset] = 0
                val layer = subject.layer
                val map = layer.toMap()
                assertThat(map, equalTo(mapOf(
                    subject.nameOf(spec.size) to 4,
                    subject.nameOf(spec.type) to NetworkBuffer.Type.ON_HEAP.name,
                    subject.nameOf(spec.offset) to 0)))
            }
        }
        on("set with item") {
            subject[NetworkBuffer.name] = "newName"
            it("should contain the specified value in the top level," +
                " and keep the rest levels unchanged") {
                assertThat(subject[NetworkBuffer.name], equalTo("newName"))
                assertThat(subject.parent!![NetworkBuffer.name], equalTo("buffer"))
            }
        }
        on("set with name") {
            subject[subject.nameOf(NetworkBuffer.name)] = "newName"
            it("should contain the specified value in the top level," +
                " and keep the rest levels unchanged") {
                assertThat(subject[NetworkBuffer.name], equalTo("newName"))
                assertThat(subject.parent!![NetworkBuffer.name], equalTo("buffer"))
            }
        }
        on("set parent's value") {
            subject.parent!![NetworkBuffer.name] = "newName"
            it("should contain the specified value in both top and parent level") {
                assertThat(subject[NetworkBuffer.name], equalTo("newName"))
                assertThat(subject.parent!![NetworkBuffer.name], equalTo("newName"))
            }
        }
        on("add spec") {
            val spec = object : ConfigSpec(NetworkBuffer.prefix) {
                val minSize by optional(1)
            }
            subject.addSpec(spec)
            it("should contain items in new spec, and keep the rest level unchanged") {
                assertThat(spec.minSize in subject, equalTo(true))
                assertThat(subject.nameOf(spec.minSize) in subject, equalTo(true))
                assertThat(spec.minSize !in subject.parent!!, equalTo(true))
                assertThat(subject.nameOf(spec.minSize) !in subject.parent!!, equalTo(true))
            }
        }
        on("add spec to parent") {
            val spec = object : ConfigSpec(NetworkBuffer.prefix) {
                @Suppress("unused")
                val minSize by optional(1)
            }
            it("should throw LayerFrozenException") {
                assertThat({ subject.parent!!.addSpec(spec) }, throws<LayerFrozenException>())
            }
        }
        on("add item to parent") {
            val minSize by Spec.dummy.optional(1)
            it("should throw LayerFrozenException") {
                assertThat({ subject.parent!!.addItem(minSize) }, throws<LayerFrozenException>())
            }
        }
        on("add source") {
            val source1 = mapOf(subject.nameOf(NetworkBuffer.size) to 10).asKVSource()
            subject.addSource(source1)
            val source2 = mapOf(subject.nameOf(NetworkBuffer.size) to 20).asKVSource()
            subject.parent!!.addSource(source2)
            val source3 = mapOf(subject.nameOf(NetworkBuffer.size) to 30).asKVSource()
            subject.addSource(source3)
            it("should load values to the corresponding layers") {
                assertThat(subject[NetworkBuffer.size], equalTo(30))
                assertThat(subject.parent!![NetworkBuffer.size], equalTo(20))
                assertThat(
                    subject.sources.toList(),
                    equalTo(ArrayDeque(listOf(source3, source1, source2)).toList<Source>()))
                assertThat(
                    subject.parent!!.sources.toList(),
                    equalTo(ArrayDeque(listOf(source2)).toList<Source>()))
            }
        }
        on("iterate items in config after adding spec") {
            val spec = object : ConfigSpec(NetworkBuffer.prefix) {
                @Suppress("unused")
                val minSize by optional(1)
            }
            subject.addSpec(spec)
            it("should cover all items in config") {
                assertThat(subject.iterator().asSequence().toSet(),
                    equalTo((NetworkBuffer.items + spec.items).toSet()))
            }
        }
        on("add custom deserializer to mapper in parent") {
            it("should throw LoadException before adding deserializer") {
                val spec = object : ConfigSpec() {
                    @Suppress("unused")
                    val item by required<StringWrapper>()
                }
                val parent = Config { addSpec(spec) }
                val child = parent.withLayer("child")

                assertThat(parent.mapper, sameInstance(child.mapper))
                assertThat({ child.from.map.kv(mapOf("item" to "string")) },
                    throws<LoadException>())
            }
            it("should be able to use the specified deserializer after adding") {
                val spec = object : ConfigSpec() {
                    val item by required<StringWrapper>()
                }
                val parent = Config { addSpec(spec) }
                val child = parent.withLayer("child")

                assertThat(parent.mapper, sameInstance(child.mapper))
                parent.mapper.registerModule(SimpleModule().apply {
                    addDeserializer(StringWrapper::class.java, StringWrapperDeserializer())
                })
                val afterLoad = child.from.map.kv(mapOf("item" to "string"))
                assertThat(child.mapper, sameInstance(afterLoad.mapper))
                assertThat(afterLoad[spec.item], equalTo(StringWrapper("string")))
            }
        }
    }
})

private data class StringWrapper(val string: String)

private class StringWrapperDeserializer :
    StdDeserializer<StringWrapper>(StringWrapper::class.java) {
    override fun deserialize(
        jp: JsonParser,
        ctxt: DeserializationContext
    ): StringWrapper {
        val node = jp.codec.readTree<JsonNode>(jp)
        return StringWrapper(node.textValue())
    }
}
