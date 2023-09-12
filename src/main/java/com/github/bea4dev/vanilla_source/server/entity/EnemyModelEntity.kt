package com.github.bea4dev.vanilla_source.server.entity

import com.github.bea4dev.vanilla_source.server.coroutine.launch
import com.github.bea4dev.vanilla_source.server.coroutine.tick
import com.github.bea4dev.vanilla_source.server.entity.EnemyModelEntity.AttackingStatus.*
import com.github.bea4dev.vanilla_source.server.entity.ai.EntityAIController
import com.github.bea4dev.vanilla_source.server.item.VanillaSourceItem
import com.github.bea4dev.vanilla_source.server.item.WeaponItem
import com.github.bea4dev.vanilla_source.util.math.createCube
import com.github.bea4dev.vanilla_source.util.math.getEntitiesInBox
import com.github.bea4dev.vanilla_source.util.math.normalizeDegrees
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.minestom.server.adventure.audience.Audiences
import net.minestom.server.collision.BoundingBox
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityCreature
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.thread.TickThread
import team.unnamed.hephaestus.Model
import team.unnamed.hephaestus.minestom.MinestomModelEngine.BoneType
import team.unnamed.hephaestus.minestom.ModelEntity
import kotlin.math.abs
import kotlin.random.Random

private const val tickRotationDelta = 9.0F

@Suppress("UnstableApiUsage")
open class EnemyModelEntity(entityType: EntityType, model: Model)
    : ModelEntity(entityType, model, BoneType.AREA_EFFECT_CLOUD), VanillaSourceEntity {

    private var currentJob: Job? = null
    private var currentAction: (suspend CoroutineScope.() -> Unit)? = null
    protected open val attackActions: AttackActions? = getDefaultAttackingActions()
    protected var attackingStatus = IDLE
    protected var comboCount = 0

    private var yawGoal = super.position.yaw
    private var pitchGoal = super.position.pitch

    private var thread: TickThread? = null

    @Suppress("LeakingThis")
    val aiController = EntityAIController(this)

    init {
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
            this.launch {
                if (attackingStatus == IDLE || attackingStatus == GUARD) {
                    comboCount = 0
                    //Audiences.players().sendMessage(Component.text("attack : $attackingStatus"))
                    attackActions.attackAction(this)
                }
            }
        }
    }

    protected var previousAnimation: String? = null
    override fun playAnimation(name: String?) {
        if (name != previousAnimation) {
            Audiences.players().sendMessage(Component.text("play : $name"))
            super.playAnimation(name)
            previousAnimation = name
        }
    }

    protected fun playIdleAnimation() {
        if (attackingStatus == IDLE) {
            if (super.position.asVec().distanceSquared(super.previousPosition.asVec()) < 0.01) {
                playAnimation("idle")
            } else {
                playAnimation("walk")
            }
        }
    }

    override fun tick(time: Long) {
        val thread = Thread.currentThread()
        if (thread !is TickThread) {
            throw IllegalStateException("Unsafe tick!")
        }
        this.thread = thread

        this.aiController.tick(super.position)
        super.tick(time)
        this.playIdleAnimation()

        // Delayed rotation
        val currentYaw = super.position.yaw
        val currentPitch = super.position.pitch
        val yawDelta = normalizeDegrees(this.yawGoal - currentYaw)
        val pitchDelta = normalizeDegrees(this.pitchGoal - currentPitch)
        val yaw = if (abs(yawDelta) <= tickRotationDelta) {
            this.yawGoal
        } else {
            if (yawDelta > 0.0F) {
                currentYaw + tickRotationDelta
            } else {
                currentYaw - tickRotationDelta
            }
        }
        val pitch = if (abs(pitchDelta) <= tickRotationDelta) {
            this.pitchGoal
        } else {
            if (pitchDelta > 0.0F) {
                currentPitch + tickRotationDelta
            } else {
                currentPitch - tickRotationDelta
            }
        }
        super.setView(yaw, pitch)
    }

    fun onAttacked(source: Player, item: VanillaSourceItem) {
        if (item !is WeaponItem || !item.isMelee) {
            return
        }

        this.launch {
            val attackActions = attackActions ?: return@launch

            when (attackingStatus) {
                ATTACKING -> {
                    if (comboCount == 0) {
                        attackActions.guardedAction(this)
                    }
                }
                ATTACKING_COMBO -> {
                    attackingStatus = COMBO_GUARDED
                    comboCount--
                }
                ATTACK_PRELIMINARY -> {
                    damage(DamageType.fromPlayer(source), item.damage)
                }
                GUARDED -> {
                    damage(DamageType.fromPlayer(source), item.damage)
                }
                COMBO_GUARDED -> {

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

                        setAttacking(this, 2.tick) {
                            attackAsBox(center, box, 6.0F) { entity -> entity is Player }

                            delay(7.tick)
                            Audiences.players().sendMessage(Component.text("1"))
                            attackingStatus = IDLE
                        }
                    }
                    else -> {
                        if (random == 1) {
                            playAnimation("attack2")
                        } else {
                            playAnimation("attack3")
                        }
                        attackingStatus = ATTACK_PRELIMINARY
                        delay(18.tick)

                        setComboAttacking()
                        delay(2.tick)
                        attackAsBox(center, box, 4.0F) { entity -> entity is Player }

                        attackingStatus = ATTACK_PRELIMINARY
                        delay(18.tick)

                        if (random == 1) {
                            setAttacking(this, 2.tick) {
                                attackAsBox(center, box, 4.0F) { entity -> entity is Player }

                                delay(7.tick)
                                Audiences.players().sendMessage(Component.text("2"))
                                attackingStatus = IDLE
                            }
                        } else {
                            setComboAttacking()
                            delay(2.tick)
                            attackAsBox(center, box, 4.0F) { entity -> entity is Player }

                            attackingStatus = ATTACK_PRELIMINARY
                            delay(18.tick)

                            setAttacking(this, 2.tick) {
                                attackAsBox(center, box, 6.0F) { entity -> entity is Player }

                                delay(7.tick)
                                Audiences.players().sendMessage(Component.text("2"))
                                attackingStatus = IDLE
                            }
                        }
                    }
                }
            },
            {
                playAnimation("guard")
                attackingStatus = GUARD
                stopNavigation()

                delay(20.tick)
                Audiences.players().sendMessage(Component.text("3"))
                attackingStatus = IDLE
                startNavigation()
            },
            {
                playAnimation("guarded-long")
                attackingStatus = GUARDED
                stopNavigation()

                delay(60.tick)
                attackingStatus = IDLE
                Audiences.players().sendMessage(Component.text("4"))
                startNavigation()
            }
        )
    }

    fun stopNavigation() {
        aiController.navigator.enable = false
    }

    fun startNavigation() {
        aiController.navigator.enable = true
    }

    protected suspend fun setAttacking(scope: CoroutineScope, delay: Long, onSuccess: suspend CoroutineScope.() -> Unit) {
        attackingStatus = ATTACKING
        delay(delay)
        if (attackingStatus == ATTACKING) {
            onSuccess(scope)
        }
    }

    protected fun setComboAttacking() {
        attackingStatus = ATTACKING_COMBO
        comboCount++
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

    fun setViewDelayed(yaw: Float, pitch: Float) {
        this.yawGoal = yaw
        this.pitchGoal = pitch
    }

    data class AttackActions(
        var attackAction: suspend CoroutineScope.() -> Unit,
        var guardAction: suspend CoroutineScope.() -> Unit,
        var guardedAction: suspend CoroutineScope.() -> Unit,
    )

    enum class AttackingStatus {
        ATTACKING,
        ATTACKING_COMBO,
        ATTACK_PRELIMINARY,
        COMBO_GUARDED,
        GUARDED,
        GUARD,
        IDLE
    }

    override fun getTickThread(): TickThread? {
        return this.thread
    }

}