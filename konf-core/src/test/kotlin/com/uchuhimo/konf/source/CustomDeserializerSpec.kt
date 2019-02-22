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

package com.uchuhimo.konf.source

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.jetbrains.spek.subject.SubjectSpek

@JsonDeserialize(using = SealedClassDeserializer::class)
sealed class SealedClass

data class VariantA(val int: Int) : SealedClass()
data class VariantB(val double: Double) : SealedClass()

class SealedClassDeserializer : StdDeserializer<SealedClass>(SealedClass::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): SealedClass {
        val node: JsonNode = p.codec.readTree(p)
        return if (node.has("int")) {
            VariantA(node.get("int").asInt())
        } else {
            VariantB(node.get("double").asDouble())
        }
    }
}

object CustomDeserializerConfig : ConfigSpec("level1.level2") {
    val variantA by required<SealedClass>()
    val variantB by required<SealedClass>()
}

object CustomDeserializerSpec : SubjectSpek<Config>({

    subject {
        Config {
            addSpec(CustomDeserializerConfig)
        }.from.map.kv(loadContent)
    }
    given("a source") {
        on("load the source into config") {
            it("should contain every value specified in the source") {
                val variantA = VariantA(1)
                val variantB = VariantB(2.0)
                assertThat(subject[CustomDeserializerConfig.variantA], equalTo<SealedClass>(variantA))
                assertThat(subject[CustomDeserializerConfig.variantB], equalTo<SealedClass>(variantB))
            }
        }
    }
})

private val loadContent = mapOf<String, Any>(
    "variantA" to mapOf("int" to 1),
    "variantB" to mapOf("double" to 2.0)
).mapKeys { (key, _) -> "level1.level2.$key" }
