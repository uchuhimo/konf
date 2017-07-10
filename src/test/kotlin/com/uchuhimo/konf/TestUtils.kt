package com.uchuhimo.konf

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import java.io.File

fun tempFileOf(content: String, prefix: String = "tmp", suffix: String = ".tmp"): File {
    return createTempFile(prefix, suffix).apply {
        writeText(content)
    }
}

fun assertTrue(actual: Boolean) {
    assertThat(actual, equalTo(true))
}

fun assertFalse(actual: Boolean) {
    assertThat(actual, equalTo(false))
}
