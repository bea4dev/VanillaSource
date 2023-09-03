package com.github.bea4dev.vanilla_source.server.entity

import com.github.bea4dev.vanilla_source.server.coroutine.launch
import com.github.bea4dev.vanilla_source.server.entity.EnemyModelEntity.AttackingStatus.*
import com.github.bea4dev.vanilla_source.server.item.VanillaSourceItem
import com.github.bea4dev.vanilla_source.server.item.WeaponItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.DamageType
import team.unnamed.hephaestus.Model
import team.unnamed.hephaestus.minestom.MinestomModelEngine.BoneType
import team.unnamed.hephaestus.minestom.ModelEntity

open class EnemyModelEntity(entityType: EntityType, model: Model, boneType: BoneType): ModelEntity(entityType, model, boneType) {
    private var currentJob: Job? = null
    private var currentAction: (suspend CoroutineScope.() -> Unit)? = null
    protected open val attackActions: EnemyActionProvider? = null
    protected var attackingStatus = IDLE

    fun tryAction(action: suspend CoroutineScope.() -> Unit): (suspend CoroutineScope.() -> Unit)? {
        val job = currentJob
        return if (job == null) {
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

    fun onAttacked(source: Player, item: VanillaSourceItem) {
        if (item !is WeaponItem || !item.isMelee) {
            return
        }

        this.launch {
            val attackActions = attackActions ?: return@launch

            when (attackingStatus) {
                ATTACKING -> {
                    attackActions.guardedAction(this)
                }
                ATTACK_PRELIMINARY -> {
                    damage(DamageType.fromPlayer(source), item.damage)
                }
                GUARDED -> {
                    damage(DamageType.fromPlayer(source), item.damage)
                }
                GUARD -> {
                    // None
                }
                IDLE -> {

                }
            }
        }
    }

    override fun damage(type: DamageType, value: Float): Boolean {
        colorize(255, 0, 0)
        super.scheduleNextTick { colorizeDefault() }
        return super.damage(type, value)
    }

    data class EnemyActionProvider(
        val attackAction: suspend CoroutineScope.() -> Unit,
        val guardAction: suspend CoroutineScope.() -> Unit,
        val guardedAction: suspend CoroutineScope.() -> Unit,
    )

    enum class AttackingStatus {
        ATTACKING,
        ATTACK_PRELIMINARY,
        GUARDED,
        GUARD,
        IDLE
    }

}