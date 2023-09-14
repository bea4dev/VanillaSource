package com.github.bea4dev.vanilla_source.server.entity

import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.thread.TickThread

@Suppress("UnstableApiUsage")
interface VanillaSourceEntity {
    fun getTickThread(): TickThread?
}

fun Entity.getViewPosition(positionRelative: Vec, fromEye: Boolean = true): Vec {
    val yAdd = if (fromEye) { this.eyeHeight } else { 0.0 }
    val position = positionRelative.add(0.0, yAdd, 0.0).rotateFromView(this.position)
    return this.position.asVec().add(position.x, position.y, position.z)
}