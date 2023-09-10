package com.github.bea4dev.vanilla_source.server.entity.ai.goal

import com.github.bea4dev.vanilla_source.server.entity.ai.EntityNavigator
import com.github.bea4dev.vanilla_source.server.entity.move
import com.github.bea4dev.vanilla_source.server.level.util.asBlockPosition
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityCreature
import kotlin.random.Random

private const val INTERVAL = 5L

class EntityTargetAttackGoal(
    private val entity: Entity,
    private val target: Entity,
    private val range: Double,
    private val attackIntervalTicks: Long
) : PathfindingGoal {

    private val randomTickAdditional = Random.nextLong(20)
    private var endOfAttackCoolTime = 0L

    override fun run(goalSelector: GoalSelector, navigator: EntityNavigator, time: Long): Boolean {
        if (target.position.asBlockPosition() == entity.position.asBlockPosition()) {
            val velocity = target.position.asVec().sub(entity.position)
            if (velocity.lengthSquared() == 0.0) {
                return true
            }
            entity.move(velocity.normalize().mul(navigator.speed.toDouble()))
            return true
        }

        if (time > endOfAttackCoolTime && target.getDistanceSquared(entity) <= range * range && entity is EntityCreature) {
            entity.attack(target)
            endOfAttackCoolTime = time + attackIntervalTicks
        }

        if ((time + randomTickAdditional) % INTERVAL != 0L) {
            return true
        }

        val position = target.position
        navigator.setNavigationGoal(position.asBlockPosition())

        return true
    }
}