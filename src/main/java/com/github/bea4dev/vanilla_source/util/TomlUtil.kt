package com.github.bea4dev.vanilla_source.util

import com.moandjiezana.toml.Toml
import net.minestom.server.coordinate.Pos

fun Toml.asPosition(): Pos? {
    return if (this.contains("x") || this.contains("y") || this.contains("z")) {
        val x = this.getDouble("x") ?: 0.0
        val y = this.getDouble("y") ?: 0.0
        val z = this.getDouble("z") ?: 0.0
        Pos(x, y, z)
    } else {
        null
    }
}