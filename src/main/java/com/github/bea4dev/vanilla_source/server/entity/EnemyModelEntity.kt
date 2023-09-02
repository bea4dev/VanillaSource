package com.github.bea4dev.vanilla_source.server.entity

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import net.minestom.server.entity.EntityType
import team.unnamed.hephaestus.Model
import team.unnamed.hephaestus.minestom.MinestomModelEngine.BoneType
import team.unnamed.hephaestus.minestom.ModelEntity
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Consumer
import kotlin.concurrent.withLock

class EnemyModelEntity(entityType: EntityType, model: Model, boneType: BoneType): ModelEntity(entityType, model, boneType) {
    private var currentJob: Job? = null
    private var currentAction: (suspend CoroutineScope.() -> Unit)? = null
    private val actionLock = ReentrantLock()

    private fun trySetAction(action: suspend CoroutineScope.() -> Unit): (suspend CoroutineScope.() -> Unit)? {
        return actionLock.withLock {
            val job = currentJob
            if (job == null) {
                currentAction = action
                null
            } else {
                if (job.isCompleted) {
                    currentAction = action
                    null
                } else {
                    currentAction
                }
            }
        }
    }

    fun action(action: suspend CoroutineScope.() -> Unit, onFailure: Consumer<suspend CoroutineScope.() -> Unit>) {
        val currentAction = this.trySetAction(action)
        if (currentAction != null) {
            onFailure.accept(currentAction)
        }
    }

}