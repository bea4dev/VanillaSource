package com.github.bea4dev.vanilla_source.config.types

import net.minestom.server.coordinate.Pos

data class Position(val x: Double, val y: Double, val z: Double, val yaw: Float?, val pitch: Float?) {

    fun toPos(): Pos = Pos(x, y, z, yaw ?: 0.0F, pitch ?: 0.0F)

}
