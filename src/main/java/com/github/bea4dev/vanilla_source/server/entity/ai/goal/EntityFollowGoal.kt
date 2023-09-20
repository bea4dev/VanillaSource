package com.github.bea4dev.vanilla_source.server.entity.ai.goal

import com.github.bea4dev.vanilla_source.server.entity.ai.EntityNavigator
import com.github.bea4dev.vanilla_source.server.level.util.asBlockPosition
import net.minestom.server.entity.Entity
import kotlin.random.Random

private const val INTERVAL = 20L

class EntityFollowGoal(private val target: Entity) : PathfindingGoal {
    private val randomTickAdditional = Random.nextLong(INTERVAL)

    override fun run(goalSelector: GoalSelector, navigator: EntityNavigator, time: Long): Boolean {
        if ((time + randomTickAdditional) % INTERVAL != 0L) {
            return true
        }

        val position = target.position
        navigator.setNavigationGoal(position.asBlockPosition())

        return true
    }
}