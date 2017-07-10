package com.uchuhimo.konf.source.env

import com.uchuhimo.konf.source.Source
import com.uchuhimo.konf.source.base.FlatSource

object EnvProvider {
    fun fromEnv(): Source {
        return FlatSource(System.getenv(), type = "system-environment")
    }
}
