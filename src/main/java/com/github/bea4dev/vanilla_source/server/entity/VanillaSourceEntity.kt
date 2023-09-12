package com.github.bea4dev.vanilla_source.server.entity

import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.thread.TickThread

@Suppress("UnstableApiUsage")
interface VanillaSourceEntity {
    fun getTickThread(): TickThread?
}

fun Entity.getViewPosition(positionRelative: Vec, fromEye: Boolean = true): Vec {
    val position = positionRelative.rotateFromView(this.position)
    val x = position.x
    val y = if (fromEye) { position.y } else { position.y + this.eyeHeight }
    val z = position.z
    return this.position.asVec().add(x, y, z)
}