package com.github.bea4dev.vanilla_source.server.entity

import net.minestom.server.collision.CollisionUtils
import net.minestom.server.collision.PhysicsResult
import net.minestom.server.collision.Shape
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity

@Suppress("UnstableApiUsage")
fun Entity.move(velocity: Vec): PhysicsResult {
    return this.move(velocity, 0.0)
}

@Suppress("UnstableApiUsage")
fun Entity.move(velocity: Vec, autoJumpHeight: Double): PhysicsResult {
    val result = CollisionUtils.handlePhysics(this, velocity)
    this.refreshPosition(result.newPosition)
    if (autoJumpHeight == 0.0 || velocity.y < 0.0 || (!result.collisionX && !result.collisionZ)) {
        return result
    }

    val shapes: Array<out Shape?> = result.collisionShapes
    var maxY = 0.0
    for (shape in shapes) {
        if (shape == null) {
            continue
        }
        val height = shape.relativeEnd().y()
        if (height > maxY) {
            maxY = height
        }
    }

    if (maxY != 0.0 && maxY <= autoJumpHeight) {
        val jump = CollisionUtils.handlePhysics(this, Vec(0.0, maxY + 0.01, 0.0))
        if (jump.newVelocity.y >= maxY) {
            this.refreshPosition(jump.newPosition)
            return this.move(velocity.sub(result.newVelocity), 0.0)
        }
    }

    return result
}

@Suppress("UnstableApiUsage")
val Entity.isOnGroundStrict: Boolean
    get() {
        val result = CollisionUtils.handlePhysics(this, Vec(0.0, -Vec.EPSILON, 0.0))
        return result.isOnGround
    }