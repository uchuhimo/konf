@file:JvmName("Configs")

package com.uchuhimo.konf

import com.uchuhimo.konf.annotation.JavaApi
import java.util.function.Consumer

@JavaApi
fun create(): Config = Config()

@JavaApi
fun create(init: Consumer<Config>): Config = Config { init.accept(this) }
