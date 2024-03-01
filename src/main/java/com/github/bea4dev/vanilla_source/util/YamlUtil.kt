package com.github.bea4dev.vanilla_source.util

import net.minestom.server.coordinate.Pos

fun Map<String, Any>.asPosition(): Pos? {
    return if (this.containsKey("x") || this.containsKey("y") || this.containsKey("z")) {
        val x = this["x"]?.let { it as Double } ?: 0.0
        val y = this["y"]?.let { it as Double } ?: 0.0
        val z = this["z"]?.let { it as Double } ?: 0.0
        Pos(x, y, z)
    } else {
        null
    }
}