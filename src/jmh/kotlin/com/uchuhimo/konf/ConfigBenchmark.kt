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
        val name = optional(
                name = "name",
                default = "buffer",
                description = "name of buffer")
    }
}

@BenchmarkMode(AverageTime)
@OutputTimeUnit(NANOSECONDS)
class ConfigBenchmark {

    @State(Scope.Thread)
    class ConfigState {
        val config = Config { addSpec(Buffer) }
    }

    @State(Scope.Benchmark)
    class MultiThreadConfigState {
        val config = Config { addSpec(Buffer) }
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
    fun getWithName(state: ConfigState) = state.config<String>(Buffer.name.name)

    @Benchmark
    fun getWithNameFromMultiThread(state: MultiThreadConfigState) =
            state.config<String>(Buffer.name.name)

    @Benchmark
    fun setWithName(state: ConfigState) {
        state.config[Buffer.name.name] = "newName"
    }

    @Benchmark
    fun setWithNameFromMultiThread(state: MultiThreadConfigState) {
        state.config[Buffer.name.name] = "newName"
    }
}

@BenchmarkMode(AverageTime)
@OutputTimeUnit(NANOSECONDS)
class MultiLevelConfigBenchmark {

    @State(Scope.Thread)
    class ConfigState {
        val config = Config { addSpec(Buffer) }.withLayer().withLayer().withLayer().withLayer()
    }

    @State(Scope.Benchmark)
    class MultiThreadConfigState {
        val config = Config { addSpec(Buffer) }.withLayer().withLayer().withLayer().withLayer()
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
    fun getWithName(state: ConfigState) = state.config<String>(Buffer.name.name)

    @Benchmark
    fun getWithNameFromMultiThread(state: MultiThreadConfigState) =
            state.config<String>(Buffer.name.name)

    @Benchmark
    fun setWithName(state: ConfigState) {
        state.config[Buffer.name.name] = "newName"
    }

    @Benchmark
    fun setWithNameFromMultiThread(state: MultiThreadConfigState) {
        state.config[Buffer.name.name] = "newName"
    }
}
