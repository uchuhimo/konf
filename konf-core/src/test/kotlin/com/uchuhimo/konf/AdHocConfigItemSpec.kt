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

package com.uchuhimo.konf

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.toValue
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import kotlin.test.assertNull

object AdHocConfigItemSpec : Spek({
    on("load config into ad-hoc config class with ad-hoc config items") {
        val config = Config().from.map.kv(
            mapOf(
                "network.buffer.size" to 1,
                "network.buffer.heap.type" to AdHocNetworkBuffer.Type.ON_HEAP,
                "network.buffer.offset" to 0
            )
        )
        val networkBuffer = AdHocNetworkBuffer(config)
        it("should load correct values") {
            assertThat(networkBuffer.size, equalTo(1))
            assertThat(networkBuffer.maxSize, equalTo(2))
            assertThat(networkBuffer.name, equalTo("buffer"))
            assertThat(networkBuffer.type, equalTo(AdHocNetworkBuffer.Type.ON_HEAP))
            assertThat(networkBuffer.offset, equalTo(0))
        }
    }
    val configForCast = Config().from.map.hierarchical(
        mapOf(
            "size" to 1,
            "maxSize" to 2,
            "name" to "buffer",
            "type" to "ON_HEAP",
            "offset" to "null"
        )
    )
    on("cast config to config class property") {
        val networkBufferForCast: NetworkBufferForCast by configForCast.cast()
        it("should load correct values") {
            assertThat(networkBufferForCast.size, equalTo(1))
            assertThat(networkBufferForCast.maxSize, equalTo(2))
            assertThat(networkBufferForCast.name, equalTo("buffer"))
            assertThat(networkBufferForCast.type, equalTo(NetworkBufferForCast.Type.ON_HEAP))
            assertNull(networkBufferForCast.offset)
        }
    }
    on("cast config to config class") {
        val networkBufferForCast = configForCast.toValue<NetworkBufferForCast>()
        it("should load correct values") {
            assertThat(networkBufferForCast.size, equalTo(1))
            assertThat(networkBufferForCast.maxSize, equalTo(2))
            assertThat(networkBufferForCast.name, equalTo("buffer"))
            assertThat(networkBufferForCast.type, equalTo(NetworkBufferForCast.Type.ON_HEAP))
            assertNull(networkBufferForCast.offset)
        }
    }
    on("cast source to config class") {
        val networkBufferForCast = Source.from.map.hierarchical(
            mapOf(
                "size" to 1,
                "maxSize" to 2,
                "name" to "buffer",
                "type" to "ON_HEAP",
                "offset" to "null"
            )
        ).toValue<NetworkBufferForCast>()
        it("should load correct values") {
            assertThat(networkBufferForCast.size, equalTo(1))
            assertThat(networkBufferForCast.maxSize, equalTo(2))
            assertThat(networkBufferForCast.name, equalTo("buffer"))
            assertThat(networkBufferForCast.type, equalTo(NetworkBufferForCast.Type.ON_HEAP))
            assertNull(networkBufferForCast.offset)
        }
    }
})

data class NetworkBufferForCast(
    val size: Int,
    val maxSize: Int,
    val name: String,
    val type: Type,
    val offset: Int?
) {

    enum class Type {
        ON_HEAP, OFF_HEAP
    }
}
