package com.github.bea4dev.vanilla_source.server.entity.ai.goal

import com.github.bea4dev.vanilla_source.server.entity.ai.EntityAIController


class GoalSelector(controller: EntityAIController) {
    private val controller: EntityAIController
    var goals = ArrayList<PathfindingGoal>()

    init {
        this.controller = controller
    }

    fun tick(time: Long) {
        val navigator = controller.navigator
        for (goal in goals) {
            val finish = goal.run(this, navigator, time)
            if (finish) {
                break
            }
        }
    }
}
