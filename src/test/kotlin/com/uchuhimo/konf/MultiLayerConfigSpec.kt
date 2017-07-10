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
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek
import org.jetbrains.spek.subject.itBehavesLike

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
            subject[NetworkBuffer.name.name] = "newName"
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
                val minSize = optional("minSize", 1)
            }
            subject.addSpec(spec)
            it("should contain items in new spec, and keep the rest level unchanged") {
                assertThat(spec.minSize in subject, equalTo(true))
                assertThat(spec.minSize.name in subject, equalTo(true))
                assertThat(spec.minSize !in subject.parent!!, equalTo(true))
                assertThat(spec.minSize.name !in subject.parent!!, equalTo(true))
            }
        }
        on("add spec to parent") {
            val spec = object : ConfigSpec(NetworkBuffer.prefix) {
                val minSize = optional("minSize", 1)
            }
            it("should throw SpecFrozenException") {
                assertThat({ subject.parent!!.addSpec(spec) }, throws<SpecFrozenException>())
            }
        }
        on("iterate items in config after adding spec") {
            val spec = object : ConfigSpec(NetworkBuffer.prefix) {
                val minSize = optional("minSize", 1)
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
                    val item = required<StringWrapper>("item")
                }
                val parent = Config { addSpec(spec) }
                val child = parent.withLayer("child")

                assertThat(parent.mapper, sameInstance(child.mapper))
                assertThat({ child.loadFrom.map.kv(mapOf("item" to "string")) },
                        throws<LoadException>())
            }
            it("should be able to use the specified deserializer after adding") {
                val spec = object : ConfigSpec() {
                    val item = required<StringWrapper>("item")
                }
                val parent = Config { addSpec(spec) }
                val child = parent.withLayer("child")

                assertThat(parent.mapper, sameInstance(child.mapper))
                parent.mapper.registerModule(SimpleModule().apply {
                    addDeserializer(StringWrapper::class.java, StringWrapperDeserializer())
                })
                val afterLoad = child.loadFrom.map.kv(mapOf("item" to "string"))
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
