package com.github.bea4dev.vanilla_source.util.math

import net.minestom.server.collision.BoundingBox
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.instance.Instance
import kotlin.math.pow
import kotlin.math.sqrt

@Suppress("UnstableApiUsage")
fun BoundingBox.getEntitiesInBox(level: Instance, basePosition: Pos): List<Entity> {
    val radius = sqrt(this.depth().pow(2) + this.height().pow(2) + this.width().pow(2))
    return level.getNearbyEntities(basePosition, radius).filter { entity -> this.intersectEntity(basePosition, entity) }
}

fun createCube(minRadius: Double): BoundingBox {
    val side = minRadius * 2.0
    return BoundingBox(side, side, side)
}