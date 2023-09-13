package com.github.bea4dev.vanilla_source.server.entity.ai

import com.github.bea4dev.vanilla_source.server.entity.EnemyModelEntity
import com.github.bea4dev.vanilla_source.server.entity.ai.goal.GoalSelector
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity

class EntityAIController(entity: Entity) {
    val entity: Entity
    val navigator: EntityNavigator
    val goalSelector: GoalSelector
    private var ticks = 0L

    init {
        this.entity = entity
        navigator = EntityNavigator(entity, 0.2F, 1.5, 0.7, 3)
        goalSelector = GoalSelector(this)
    }

    fun tick(position: Pos) {
        if (entity is EnemyModelEntity && entity.isPlayingDieAnimation) {
            return
        }
        goalSelector.tick(ticks)
        navigator.tick(position)
        ticks++
    }
}
