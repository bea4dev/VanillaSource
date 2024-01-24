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
import kotlinx.coroutines.*
import net.kyori.adventure.sound.Sound
import net.minestom.server.adventure.AdventurePacketConvertor
import net.minestom.server.collision.BoundingBox
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityCreature
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.LivingEntity
import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.entity.damage.EntityDamage
import net.minestom.server.network.packet.server.SendablePacket
import net.minestom.server.network.packet.server.play.HitAnimationPacket
import net.minestom.server.particle.Particle
import net.minestom.server.particle.ParticleCreator
import net.minestom.server.sound.SoundEvent
import net.minestom.server.thread.TickThread
import net.minestom.server.timer.TaskSchedule
import team.unnamed.hephaestus.Model
import team.unnamed.hephaestus.minestom.MinestomModelEngine.BoneType
import team.unnamed.hephaestus.minestom.ModelEntity
import kotlin.math.abs
import kotlin.random.Random

private const val tickRotationDelta = 20.0F

@Suppress("UnstableApiUsage")
open class EnemyModelEntity(entityType: EntityType, model: Model)
    : ModelEntity(entityType, model, BoneType.AREA_EFFECT_CLOUD), VanillaSourceEntity {

    private var currentJob: Job? = null
    private var currentAction: (suspend CoroutineScope.() -> Unit)? = null
    protected open val attackActions: AttackActions? = getDefaultAttackingActions()
    protected var attackingStatus = IDLE
    protected var comboCount = 0
    protected var cancelAttacking = false

    protected open val attackKnockBackStrength = 13.0
    protected open val damageSound = Sound.sound(SoundEvent.ENTITY_ZOMBIE_HURT, Sound.Source.VOICE, 0.8F, 0.7F)
    protected open val deathSound = Sound.sound(SoundEvent.ENTITY_ZOMBIE_DEATH, Sound.Source.VOICE, 0.8F, 0.7F)

    private var yawGoal = super.position.yaw
    private var pitchGoal = super.position.pitch
    private var previousPositionStrict = super.position

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
                if (attackingStatus == DEAD) {
                    return@launch
                }
                if (attackingStatus == IDLE || attackingStatus == GUARD) {
                    comboCount = 0
                    attackingStatus = ATTACK_PRELIMINARY
                    this@EnemyModelEntity.launch(block = attackActions.attackAction)
                }
            }
        }
    }

    protected var previousAnimation: String? = null
    var isPlayingDieAnimation = false
    override fun playAnimation(name: String?) {
        if (name != previousAnimation && !isPlayingDieAnimation) {
            if (name == "die") {
                isPlayingDieAnimation = true
            }
            super.playAnimation(name)
            previousAnimation = name
        }
    }

    protected fun playIdleAnimation() {
        if (attackingStatus == IDLE) {
            if (super.position.asVec().distanceSquared(this.previousPositionStrict.asVec()) < Vec.EPSILON) {
                playAnimation("idle")
            } else {
                playAnimation("walk")
            }
        }
    }

    protected fun onGuarded(player: Player, item: WeaponItem) {
        val distance = super.position.distance(player.position)
        val position = player.position.add(0.0, player.eyeHeight, 0.0).add(player.position.direction().mul(distance / 2.0))
        val packets = mutableListOf<SendablePacket>()
        for (i in 0..<10) {
            val size = 2.5
            val x = Random.nextDouble(size) - (size / 2.0)
            val y = Random.nextDouble(size) - (size / 2.0)
            val z = Random.nextDouble(size) - (size / 2.0)
            val particle = ParticleCreator.createParticlePacket(
                Particle.CRIT,
                false,
                position.x,
                position.y,
                position.z,
                x.toFloat(),
                y.toFloat(),
                z.toFloat(),
                1.0F,
                0,
                null
            )
            packets += particle
        }

        val velocity = super.position.direction().mul(attackKnockBackStrength)
        player.velocity = velocity

        val guardedSound = item.guardSound
        val sound = if (attackingStatus == ATTACKING && comboCount == 0) {
            super.velocity = velocity.neg()
            Sound.sound(guardedSound.name(), guardedSound.source(), guardedSound.volume(), guardedSound.pitch() * 1.1F)
        } else {
            guardedSound
        }
        packets += AdventurePacketConvertor.createSoundPacket(sound, position.x, position.y, position.z)
        super.sendPacketsToViewers(packets)
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
        tickRotation()
        this.previousPositionStrict = super.position
    }

    private fun tickRotation() {
        // Delayed rotation
        val tickRotationDelta = if (attackingStatus == ATTACKING || attackingStatus == ATTACKING_COMBO || attackingStatus == ATTACK_PRELIMINARY) {
            tickRotationDelta * 0.2F
        } else {
            tickRotationDelta
        }
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
                    onGuarded(source, item)
                    if (comboCount == 0) {
                        attackingStatus = GUARDED
                        this@EnemyModelEntity.launch(block = attackActions.guardedAction)
                    } else {
                        cancelAttacking = true
                    }
                }
                ATTACKING_COMBO -> {
                    onGuarded(source, item)
                    attackingStatus = COMBO_GUARDED
                    comboCount--
                }
                ATTACK_PRELIMINARY -> {
                    damage(DamageType.PLAYER_ATTACK, item.damage * 0.2F)
                }
                GUARDED -> {
                    val sound = Sound.sound(SoundEvent.ENTITY_PLAYER_ATTACK_CRIT, Sound.Source.PLAYER, 0.8F, 1.0F)
                    val position = super.position
                    super.sendPacketToViewers(AdventurePacketConvertor.createSoundPacket(sound, position.x, position.y, position.z))
                    damage(DamageType.PLAYER_ATTACK, item.damage)
                }
                COMBO_GUARDED -> {
                    damage(DamageType.PLAYER_ATTACK, item.damage * 0.2F)
                }
                DEAD -> {/*None*/}
                else -> {
                    damage(DamageType.PLAYER_ATTACK, item.damage / 8.0F)
                    if (!isPlayingDieAnimation) {
                        this@EnemyModelEntity.launch(block = attackActions.guardAction)
                    }
                }
            }
        }
    }

    fun getDefaultAttackingActions(): AttackActions {
        return AttackActions(
            {
                when (val random = Random.nextInt(3)) {
                    0 -> {
                        playAnimation("attack")
                        attackingStatus = ATTACK_PRELIMINARY
                        delay(17.tick)

                        setAttacking(3.tick) {
                            attackAsSphere(getViewPosition(Vec(1.0, -0.5, 0.0)).asPosition(), 3.0, 45.0, 6.0F) { entity -> entity is Player }

                            delay(7.tick)
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
                        delay(17.tick)

                        setComboAttacking()
                        delay(3.tick)
                        if (attackingStatus == ATTACKING_COMBO) {
                            attackAsSphere(
                                getViewPosition(Vec(1.0, -0.5, 0.0)).asPosition(),
                                3.0,
                                45.0,
                                4.0F
                            ) { entity -> entity is Player }
                        }

                        attackingStatus = ATTACK_PRELIMINARY
                        delay(16.tick)

                        if (random == 1) {
                            setAttacking(3.tick) {
                                attackAsSphere(getViewPosition(Vec(1.0, -0.5, 0.0)).asPosition(), 3.0, 45.0, 6.0F) { entity -> entity is Player }

                                delay(8.tick)
                                attackingStatus = IDLE
                            }
                        } else {
                            setComboAttacking()
                            delay(3.tick)
                            if (attackingStatus == ATTACKING_COMBO) {
                                attackAsSphere(
                                    getViewPosition(Vec(1.0, -0.5, 0.0)).asPosition(),
                                    3.0,
                                    45.0,
                                    4.0F
                                ) { entity -> entity is Player }
                            }

                            attackingStatus = ATTACK_PRELIMINARY
                            delay(15.tick)

                            setAttacking(3.tick) {
                                attackAsSphere(getViewPosition(Vec(1.0, -0.5, 0.0)).asPosition(), 3.0, 45.0, 6.0F) { entity -> entity is Player }

                                delay(8.tick)
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
                if (attackingStatus == GUARD) {
                    attackingStatus = IDLE
                }
                startNavigation()
            },
            {
                playAnimation("guarded-long")
                attackingStatus = GUARDED
                stopNavigation()

                delay(60.tick)
                attackingStatus = IDLE
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

    protected suspend fun setAttacking(delay: Long, onSuccess: suspend CoroutineScope.() -> Unit) = coroutineScope {
        attackingStatus = ATTACKING
        val velocity = super.position.direction().mul(attackKnockBackStrength * 0.6)
        super.velocity = super.velocity.add(velocity)
        delay(delay)
        if (attackingStatus == ATTACKING) {
            attackingStatus = ATTACK_PRELIMINARY
            onSuccess()
        }
    }

    protected fun setComboAttacking() {
        attackingStatus = ATTACKING_COMBO
        comboCount++
        val velocity = super.position.direction().mul(attackKnockBackStrength * 0.5)
        super.velocity = super.velocity.add(velocity)
    }

    fun attackAsSphere(center: Pos, radius: Double, maxAngle: Double, damage: Float, filter: (Entity) -> Boolean) {
        if (cancelAttacking) {
            cancelAttacking = false
            return
        }
        val entities = super.instance.getNearbyEntities(center, radius).filter(filter)
        for (entity in entities) {
            if (entity == this || entity !is LivingEntity) {
                continue
            }
            val direction1 = super.position.direction()
            val direction2 = entity.position.sub(super.position).asVec()
            if (direction1.angle(direction2) > maxAngle) {
                continue
            }
            entity.damage(DamageType.MOB_ATTACK, damage)
            entity.velocity = direction1.mul(attackKnockBackStrength)
            if (entity is Player) {
                entity.sendPacket(HitAnimationPacket(entity.entityId, Pos.ZERO.withDirection(direction1).yaw))
            }
        }
    }

    override fun damage(type: DamageType, value: Float): Boolean {
        if (isPlayingDieAnimation) {
            return false
        }
        colorize(255, 0, 0)
        super.scheduler().scheduleTask({ colorizeDefault() }, TaskSchedule.tick(2), TaskSchedule.stop())
        if (super.getHealth() <= value) {
            attackingStatus = DEAD
            playAnimation("die")
            val position = super.position
            super.sendPacketToViewers(AdventurePacketConvertor.createSoundPacket(this.deathSound, position.x, position.y, position.z))

            val velocity = if (type is EntityDamage) {
                val entity = type.source
                entity.position.direction()
            } else {
                val direction = super.position.direction()
                direction.neg()
            }
            super.velocity = velocity.mul(20.0)

            super.scheduler().scheduleTask({ kill() }, TaskSchedule.tick(30), TaskSchedule.stop())
            return true
        }
        super.setHealth(super.getHealth() - value)
        super.sendPacketToViewers(AdventurePacketConvertor.createSoundPacket(this.damageSound, position.x, position.y, position.z))
        return true
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
        IDLE,
        DEAD
    }

    override fun getTickThread(): TickThread? {
        return this.thread
    }

}