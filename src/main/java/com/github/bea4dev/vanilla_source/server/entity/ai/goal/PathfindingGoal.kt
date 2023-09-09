package com.github.bea4dev.vanilla_source.server.entity.ai.goal

import com.github.bea4dev.vanilla_source.server.entity.ai.EntityNavigator


interface PathfindingGoal {
    fun run(goalSelector: GoalSelector, navigator: EntityNavigator, time: Long): Boolean
}
