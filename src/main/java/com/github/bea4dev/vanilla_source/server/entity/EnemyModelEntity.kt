package com.github.bea4dev.vanilla_source.server.entity

import com.github.bea4dev.vanilla_source.server.coroutine.launch
import com.github.bea4dev.vanilla_source.server.coroutine.tick
import com.github.bea4dev.vanilla_source.server.entity.EnemyModelEntity.AttackingStatus.*
import com.github.bea4dev.vanilla_source.server.item.VanillaSourceItem
import com.github.bea4dev.vanilla_source.server.item.WeaponItem
import com.github.bea4dev.vanilla_source.util.math.createCube
import com.github.bea4dev.vanilla_source.util.math.getEntitiesInBox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import net.minestom.server.collision.BoundingBox
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityCreature
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.ai.goal.MeleeAttackGoal
import net.minestom.server.entity.ai.goal.RandomStrollGoal
import net.minestom.server.entity.ai.target.ClosestEntityTarget
import net.minestom.server.entity.ai.target.LastEntityDamagerTarget
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.utils.time.TimeUnit
import team.unnamed.hephaestus.Model
import team.unnamed.hephaestus.minestom.GenericBoneEntity
import team.unnamed.hephaestus.minestom.MinestomModelEngine.BoneType
import team.unnamed.hephaestus.minestom.ModelEntity
import java.util.function.Supplier
import kotlin.random.Random

open class EnemyModelEntity(entityType: EntityType, model: Model)
    : ModelEntity(entityType, model, BoneType.AREA_EFFECT_CLOUD), VanillaSourceEntity {

    private var currentJob: Job? = null
    private var currentAction: (suspend CoroutineScope.() -> Unit)? = null
    protected open val attackActions: AttackActions? = getDefaultAttackingActions()
    protected var attackingStatus = IDLE

    init {
        @Suppress("LeakingThis")
        super.addAIGroup(
            listOf(
                MeleeAttackGoal(this, 1.6, 20, TimeUnit.SERVER_TICK),
                RandomStrollGoal(this, 20)
            ),
            listOf(
                LastEntityDamagerTarget(this, 32.0F),
                ClosestEntityTarget(this, 32.0) { entity -> entity is Player }
            )
        )
        super.setInvisible(true)
    }

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

    override fun attack(target: Entity, swingHand: Boolean) {
        super.attack(target, swingHand)
        val attackActions = attackActions
        if (attackActions != null) {
            if (target is Player) {
                target.sendMessage("attack!")
            }
            this.launch {
                if (attackingStatus == IDLE || attackingStatus == GUARD) {
                    attackActions.attackAction(this)
                }
            }
        }
    }

    protected var previousAnimation: String? = null
    override fun playAnimation(name: String?) {
        previousAnimation = name
        super.playAnimation(name)
    }

    protected fun playIdleAnimation() {
        if (attackingStatus == IDLE) {
            if (super.position.asVec() == super.previousPosition.asVec()) {
                if (previousAnimation != "idle") {
                    playAnimation("idle")
                }
            } else {
                if (previousAnimation != "walk") {
                    playAnimation("walk")
                }
            }
        }
    }

    override fun tick(time: Long) {
        super.tick(time)
        this.playIdleAnimation()
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
                else -> {
                    damage(DamageType.fromPlayer(source), item.damage / 8.0F)
                    attackActions.guardAction(this)
                }
            }
        }
    }

    fun getDefaultAttackingActions(): AttackActions {
        return AttackActions(
            {
                val center = getViewPosition(Vec(0.0, -0.5, 0.8)).asPosition()
                val box = createCube(0.8)

                val random = Random.nextInt(3)
                when (random) {
                    0 -> {
                        playAnimation("attack")
                        attackingStatus = ATTACK_PRELIMINARY
                        delay(18.tick)

                        attackingStatus = ATTACKING
                        delay(2.tick)
                        attackAsBox(center, box, 6.0F) { entity -> entity is Player }

                        delay(7.tick)
                        attackingStatus = IDLE
                    }
                    else -> {
                        if (random == 1) {
                            playAnimation("attack2")
                        } else {
                            playAnimation("attack3")
                        }
                        attackingStatus = ATTACK_PRELIMINARY
                        delay(18.tick)

                        attackingStatus = ATTACKING
                        delay(2.tick)
                        attackAsBox(center, box, 4.0F) { entity -> entity is Player }

                        attackingStatus = ATTACK_PRELIMINARY
                        delay(18.tick)

                        attackingStatus = ATTACKING
                        delay(2.tick)
                        attackAsBox(center, box, 4.0F) { entity -> entity is Player }

                        if (random == 2) {
                            attackingStatus = ATTACK_PRELIMINARY
                            delay(18.tick)

                            attackingStatus = ATTACKING
                            delay(2.tick)
                            attackAsBox(center, box, 6.0F) { entity -> entity is Player }
                        }

                        delay(7.tick)
                        attackingStatus = IDLE
                    }
                }
            },
            {
                playAnimation("guard")
                attackingStatus = GUARD

                delay(20.tick)
                attackingStatus = IDLE
            },
            {
                playAnimation("guarded-long")
                attackingStatus = GUARDED

                delay(60.tick)
                attackingStatus = IDLE
            }
        )
    }

    fun attackAsBox(center: Pos, box: BoundingBox, damage: Float, filter: (Entity) -> Boolean) {
        val basePosition = center.sub(box.height() / 2)
        val entities = box.getEntitiesInBox(super.instance, basePosition).filter(filter)
        for (entity in entities) {
            if (entity !is EntityCreature) {
                continue
            }
            entity.damage(DamageType.fromEntity(this), damage)
        }
    }

    override fun damage(type: DamageType, value: Float): Boolean {
        colorize(255, 0, 0)
        super.scheduleNextTick { colorizeDefault() }
        return super.damage(type, value)
    }

    data class AttackActions(
        var attackAction: suspend CoroutineScope.() -> Unit,
        var guardAction: suspend CoroutineScope.() -> Unit,
        var guardedAction: suspend CoroutineScope.() -> Unit,
    )

    enum class AttackingStatus {
        ATTACKING,
        ATTACK_PRELIMINARY,
        GUARDED,
        GUARD,
        IDLE
    }

}