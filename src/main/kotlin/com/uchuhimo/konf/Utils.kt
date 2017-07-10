package com.uchuhimo.konf

fun unsupported(): Nothing {
    throw UnsupportedOperationException()
}

fun getUnits(s: String): String {
    var i = s.length - 1
    while (i >= 0) {
        val c = s[i]
        if (!c.isLetter())
            break
        i -= 1
    }
    return s.substring(i + 1)
}

fun String.notEmptyOr(default: String): String = if (isEmpty()) default else this
