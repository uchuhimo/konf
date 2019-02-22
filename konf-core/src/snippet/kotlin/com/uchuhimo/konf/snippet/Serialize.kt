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

package com.uchuhimo.konf.snippet

import com.uchuhimo.konf.Config
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

fun main(args: Array<String>) {
    val config = Config { addSpec(Server) }
    config[Server.port] = 1000
    val map = config.toMap()
    val newMap = createTempFile().run {
        ObjectOutputStream(outputStream()).use {
            it.writeObject(map)
        }
        ObjectInputStream(inputStream()).use {
            @Suppress("UNCHECKED_CAST")
            it.readObject() as Map<String, Any>
        }
    }
    val newConfig = Config {
        addSpec(Server)
    }.from.map.kv(newMap)
    check(config == newConfig)
}
