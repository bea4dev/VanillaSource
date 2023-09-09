package com.github.bea4dev.vanilla_source.server.entity.ai

import com.github.bea4dev.vanilla_source.server.entity.ai.goal.GoalSelector
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity

class EntityAIController(entity: Entity) {
    val entity: Entity
    val navigator: EntityNavigator
    val goalSelector: GoalSelector

    init {
        this.entity = entity
        navigator = EntityNavigator(entity, 0.2F, 1.5F, 3)
        goalSelector = GoalSelector(this)
    }

    fun tick(position: Pos, time: Long) {
        goalSelector.tick(time)
        navigator.tick(position)
    }
}
