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

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Mode.AverageTime
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import java.util.concurrent.TimeUnit.NANOSECONDS

class Buffer {
    companion object : ConfigSpec("network.buffer") {
        val name by optional("buffer", description = "name of buffer")
    }
}

@BenchmarkMode(AverageTime)
@OutputTimeUnit(NANOSECONDS)
class ConfigBenchmark {

    @State(Scope.Thread)
    class ConfigState {
        val config = Config { addSpec(Buffer) }
        val path = Buffer.qualify(Buffer.name)
    }

    @State(Scope.Benchmark)
    class MultiThreadConfigState {
        val config = Config { addSpec(Buffer) }
        val path = Buffer.qualify(Buffer.name)
    }

    @Benchmark
    fun getWithItem(state: ConfigState) = state.config[Buffer.name]

    @Benchmark
    fun getWithItemFromMultiThread(state: MultiThreadConfigState) = state.config[Buffer.name]

    @Benchmark
    fun setWithItem(state: ConfigState) {
        state.config[Buffer.name] = "newName"
    }

    @Benchmark
    fun setWithItemFromMultiThread(state: MultiThreadConfigState) {
        state.config[Buffer.name] = "newName"
    }

    @Benchmark
    fun getWithPath(state: ConfigState) = state.config<String>(state.path)

    @Benchmark
    fun getWithPathFromMultiThread(state: MultiThreadConfigState) = state.config<String>(state.path)

    @Benchmark
    fun setWithPath(state: ConfigState) {
        state.config[state.path] = "newName"
    }

    @Benchmark
    fun setWithPathFromMultiThread(state: MultiThreadConfigState) {
        state.config[state.path] = "newName"
    }
}

@BenchmarkMode(AverageTime)
@OutputTimeUnit(NANOSECONDS)
class MultiLevelConfigBenchmark {

    @State(Scope.Thread)
    class ConfigState {
        val config = Config { addSpec(Buffer) }.withLayer().withLayer().withLayer().withLayer()
        val path = Buffer.qualify(Buffer.name)
    }

    @State(Scope.Benchmark)
    class MultiThreadConfigState {
        val config = Config { addSpec(Buffer) }.withLayer().withLayer().withLayer().withLayer()
        val path = Buffer.qualify(Buffer.name)
    }

    @Benchmark
    fun getWithItem(state: ConfigState) = state.config[Buffer.name]

    @Benchmark
    fun getWithItemFromMultiThread(state: MultiThreadConfigState) = state.config[Buffer.name]

    @Benchmark
    fun setWithItem(state: ConfigState) {
        state.config[Buffer.name] = "newName"
    }

    @Benchmark
    fun setWithItemFromMultiThread(state: MultiThreadConfigState) {
        state.config[Buffer.name] = "newName"
    }

    @Benchmark
    fun getWithPath(state: ConfigState) = state.config<String>(state.path)

    @Benchmark
    fun getWithPathFromMultiThread(state: MultiThreadConfigState) = state.config<String>(state.path)

    @Benchmark
    fun setWithPath(state: ConfigState) {
        state.config[state.path] = "newName"
    }

    @Benchmark
    fun setWithPathFromMultiThread(state: MultiThreadConfigState) {
        state.config[state.path] = "newName"
    }
}
