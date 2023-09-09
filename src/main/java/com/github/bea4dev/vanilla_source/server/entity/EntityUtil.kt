package com.github.bea4dev.vanilla_source.server.entity

import net.minestom.server.collision.CollisionUtils
import net.minestom.server.collision.PhysicsResult
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity

@Suppress("UnstableApiUsage")
fun Entity.move(velocity: Vec): PhysicsResult {
    val result = CollisionUtils.handlePhysics(this, velocity)
    this.refreshPosition(result.newPosition)
    return result
}

@Suppress("UnstableApiUsage")
val Entity.isOnGroundStrict: Boolean
    get() {
        val result = CollisionUtils.handlePhysics(this, Vec(0.0, -Vec.EPSILON, 0.0))
        return result.isOnGround
    }