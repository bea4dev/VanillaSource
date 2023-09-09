package com.github.bea4dev.vanilla_source.server.entity.ai.goal

import com.github.bea4dev.vanilla_source.server.entity.ai.EntityNavigator
import com.github.bea4dev.vanilla_source.server.level.util.asBlockPosition
import net.minestom.server.entity.Entity

private const val INTERVAL = 10L

class EntityTargetGoal(val target: Entity) : PathfindingGoal {
    override fun run(goalSelector: GoalSelector, navigator: EntityNavigator, time: Long): Boolean {
        if (time % INTERVAL != 0L) {
            return true
        }

        val position = target.position
        navigator.setNavigationGoal(position.asBlockPosition())

        return true
    }
}