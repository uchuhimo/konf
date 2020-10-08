/*
 * Copyright 2017-2020 the original author or authors.
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

package com.uchuhimo.konf.source.deserializer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.BeanProperty
import com.fasterxml.jackson.databind.DeserializationConfig
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier
import com.fasterxml.jackson.databind.deser.ContextualDeserializer
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer
import com.fasterxml.jackson.databind.type.ArrayType
import com.fasterxml.jackson.databind.type.CollectionType
import com.fasterxml.jackson.databind.type.MapType

object EmptyStringToCollectionDeserializerModifier : BeanDeserializerModifier() {

    override fun modifyMapDeserializer(
        config: DeserializationConfig?,
        type: MapType?,
        beanDesc: BeanDescription?,
        deserializer: JsonDeserializer<*>
    ): JsonDeserializer<*>? =
        object : JsonDeserializer<Map<Any, Any>>(), ContextualDeserializer, ResolvableDeserializer {
            @Suppress("UNCHECKED_CAST")
            override fun deserialize(jp: JsonParser, ctx: DeserializationContext?): Map<Any, Any>? {
                if (!jp.isExpectedStartArrayToken && jp.hasToken(JsonToken.VALUE_STRING) && jp.text.isEmpty()) {
                    return deserializer.getEmptyValue(ctx) as Map<Any, Any>?
                }
                return deserializer.deserialize(jp, ctx) as Map<Any, Any>?
            }

            override fun createContextual(
                ctx: DeserializationContext?,
                property: BeanProperty?
            ): JsonDeserializer<*>? =
                modifyMapDeserializer(
                    config,
                    type,
                    beanDesc,
                    (deserializer as ContextualDeserializer)
                        .createContextual(ctx, property)
                )

            override fun resolve(ctx: DeserializationContext?) {
                (deserializer as? ResolvableDeserializer)?.resolve(ctx)
            }
        }

    override fun modifyCollectionDeserializer(
        config: DeserializationConfig?,
        type: CollectionType?,
        beanDesc: BeanDescription?,
        deserializer: JsonDeserializer<*>
    ): JsonDeserializer<*>? =
        object : JsonDeserializer<Collection<Any>>(), ContextualDeserializer {
            @Suppress("UNCHECKED_CAST")
            override fun deserialize(jp: JsonParser, ctx: DeserializationContext?): Collection<Any>? {
                if (!jp.isExpectedStartArrayToken && jp.hasToken(JsonToken.VALUE_STRING) && jp.text.isEmpty()) {
                    return deserializer.getEmptyValue(ctx) as Collection<Any>?
                }
                return deserializer.deserialize(jp, ctx) as Collection<Any>?
            }

            override fun createContextual(
                ctx: DeserializationContext?,
                property: BeanProperty?
            ): JsonDeserializer<*>? =
                modifyCollectionDeserializer(
                    config,
                    type,
                    beanDesc,
                    (deserializer as ContextualDeserializer)
                        .createContextual(ctx, property)
                )
        }

    override fun modifyArrayDeserializer(
        config: DeserializationConfig?,
        valueType: ArrayType?,
        beanDesc: BeanDescription?,
        deserializer: JsonDeserializer<*>
    ): JsonDeserializer<*> =
        object : JsonDeserializer<Any>(), ContextualDeserializer {
            @Suppress("UNCHECKED_CAST")
            override fun deserialize(jp: JsonParser, ctx: DeserializationContext?): Any? {
                if (!jp.isExpectedStartArrayToken && jp.hasToken(JsonToken.VALUE_STRING) && jp.text.isEmpty()) {
                    val emptyValue = deserializer.getEmptyValue(ctx)
                    return if (emptyValue is Array<*>) {
                        java.lang.reflect.Array.newInstance(valueType!!.contentType.rawClass, 0)
                    } else {
                        emptyValue
                    }
                }
                return deserializer.deserialize(jp, ctx)
            }

            override fun createContextual(
                ctx: DeserializationContext?,
                property: BeanProperty?
            ): JsonDeserializer<*>? =
                modifyArrayDeserializer(
                    config,
                    valueType,
                    beanDesc,
                    (deserializer as ContextualDeserializer)
                        .createContextual(ctx, property)
                )
        }
}
