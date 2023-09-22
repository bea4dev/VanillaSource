package com.github.bea4dev.vanilla_source.server.entity.ai

import com.github.bea4dev.vanilla_source.server.entity.EnemyModelEntity
import com.github.bea4dev.vanilla_source.server.entity.ai.astar.AStarPathfinder
import com.github.bea4dev.vanilla_source.server.entity.isOnGroundStrict
import com.github.bea4dev.vanilla_source.server.entity.move
import com.github.bea4dev.vanilla_source.server.level.util.BlockPosition
import com.github.bea4dev.vanilla_source.util.ContanUtil
import net.kyori.adventure.text.Component
import net.minestom.server.adventure.audience.Audiences
import net.minestom.server.attribute.Attribute
import net.minestom.server.collision.CollisionUtils
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.LivingEntity
import net.minestom.server.instance.Instance
import org.contan_lang.runtime.JavaContanFuture
import org.contan_lang.variables.ContanObject
import org.contan_lang.variables.primitive.ContanVoidObject
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.math.floor
import kotlin.math.sqrt


@Suppress("UnstableApiUsage", "unused")
class EntityNavigator(val entity: Entity, var speed: Float, val jumpHeight: Double, var smoothJumpHeight: Double, descendingHeight: Int) {

    private var navigationGoal: BlockPosition? = null
    private var previousNavigationGoal: BlockPosition? = null
    private var goalFuture: JavaContanFuture? = null
    private var pathfindingTask: CompletableFuture<List<BlockPosition>>? = null
    private var tickSyncTask: Runnable? = null
    private var currentPaths: List<BlockPosition>? = null
    private var currentPathIndex = 0
    private val pathfinder = AStarPathfinder(entity.instance, descendingHeight, floor(jumpHeight).toInt(), 50)

    private val isLiving = entity is LivingEntity
    private var previousLevel: WeakReference<Instance?> = WeakReference(entity.instance)

    private var pathfindingInterval = 1
    private var tick = Random().nextInt(pathfindingInterval)
    private val highAccuracy = false
    private var isAsyncPathfinding = false
    var isAvoidEntityCollision = false

    private var isJumping = false
    private var jumpGoalHeight = Double.MAX_VALUE
    private var lastVelocity = Vec.ZERO

    var enable = true
    var ignorePathfinding = false

    private var thread: Thread? = null

    @Suppress("DuplicatedCode")
    fun tick(pos: Pos) {
        if (!enable) {
            return
        }

        val thread = this.thread
        if (thread == null) {
            this.thread = Thread.currentThread()
        } else {
            if (thread != Thread.currentThread()) {
                throw IllegalStateException("wrong thread")
            }
        }

        if (this.isLiving) {
            this.speed = (this.entity as LivingEntity).getAttribute(Attribute.MOVEMENT_SPEED).baseValue
        }

        if (isJumping) {
            if (entity.velocity.y <= 0.0 || entity.position.y >= jumpGoalHeight) {
                isJumping = false
            }
            entity.refreshPosition(CollisionUtils.handlePhysics(entity, lastVelocity).newPosition)
        }


        val locX = pos.x
        val locY = pos.y
        val locZ = pos.z

        tick++
        val tickSyncTask = this.tickSyncTask
        if (tickSyncTask != null) {
            tickSyncTask.run()
            this.tickSyncTask = null
        }

        if (!entity.isOnGround) {
            return
        }

        val currentLevel = this.entity.instance
        if (!this.previousLevel.refersTo(currentLevel)) {
            this.previousLevel = WeakReference(currentLevel)
            pathfinder.updateLevel(currentLevel)
            navigationGoal = null
            currentPaths = null
            currentPathIndex = 0
            return
        }

        // Update pathfinding
        if (tick % pathfindingInterval == 0) {
            val pathfindingTask = this.pathfindingTask
            if (pathfindingTask == null) {
                updatePathfinding(
                    floor(locX).toInt(),
                    floor(locY).toInt(),
                    floor(locZ).toInt()
                )
            } else {
                if (pathfindingTask.isDone) {
                    updatePathfinding(
                        floor(locX).toInt(),
                        floor(locY).toInt(),
                        floor(locZ).toInt()
                    )
                }
            }
        }

        val currentPaths = currentPaths ?: return

        // Get next path point
        if (currentPaths.size <= currentPathIndex) {
            this.currentPaths = null
            currentPathIndex = 0
            return
        }
        var next = currentPaths[currentPathIndex]
        var nextPosition = Vec(next.x + 0.5, 0.0, next.z + 0.5)

        var velocity = nextPosition.add(Vec(-locX, 0.0, -locZ))
        var velocityLengthSquared = velocity.lengthSquared()
        if (highAccuracy) {
            if (velocityLengthSquared == 0.0) {
                currentPathIndex++
                return
            }
        } else {
            val blockX = floor(locX).toInt()
            val blockZ = floor(locZ).toInt()

            if (blockX == next.x && blockZ == next.z) {
                currentPathIndex++
                if (currentPaths.size <= currentPathIndex) {
                    this.currentPaths = null
                    currentPathIndex = 0
                    return
                }

                next = currentPaths[currentPathIndex]
                nextPosition = Vec(next.x + 0.5, 0.0, next.z + 0.5)
                velocity = nextPosition.add(-locX, 0.0, -locZ)
                velocityLengthSquared = velocity.lengthSquared()
            }
        }

        if (ignorePathfinding) {
            next = navigationGoal ?: return
            nextPosition = Vec(next.x + 0.5, 0.0, next.z + 0.5)

            velocity = nextPosition.add(Vec(-locX, 0.0, -locZ))
            velocityLengthSquared = velocity.lengthSquared()
        }

        if (velocityLengthSquared > speed * speed) {
            velocity = velocity.normalize().mul(speed.toDouble())
            move(velocity, next)
        } else {
            move(velocity, next)
            val position = entity.position.asVec()
            val velocityLength = sqrt(velocityLengthSquared)
            val nextVelocityLength = speed - velocityLength

            currentPathIndex++
            if (currentPaths.size <= currentPathIndex) {
                this.currentPaths = null
                currentPathIndex = 0
                return
            }

            next = currentPaths[currentPathIndex]
            nextPosition = Vec(next.x + 0.5, 0.0, next.z + 0.5)
            velocity = nextPosition.add(-position.x, 0.0, -position.z)
            velocity = velocity.normalize().mul(nextVelocityLength)

            entity.move(velocity, smoothJumpHeight)
        }
    }

    private fun move(velocity: Vec, next: BlockPosition) {
        val pos = entity.position.withDirection(velocity)
        if (entity is EnemyModelEntity) {
            entity.setViewDelayed(pos.yaw, pos.pitch)
        } else {
            entity.setView(pos.yaw, pos.pitch)
        }

        val result = entity.move(velocity, smoothJumpHeight)
        val failedAutoJump = (result.collisionX || result.collisionZ) && result.newVelocity.y <= 0.0
        if (failedAutoJump && floor(result.newPosition.y).toInt() < next.y && entity.isOnGroundStrict) {
            // Normal jump
            this.entity.velocity = velocity.add(0.0, this.jumpHeight * 7.0, 0.0)
            this.isJumping = true
            this.jumpGoalHeight = next.y.toDouble() + Vec.EPSILON
        }
        this.lastVelocity = velocity

        val goal = navigationGoal
        val future = goalFuture
        if (future != null && goal != null) {
            val position = entity.position
            val goalPosition = Vec(goal.x.toDouble(), goal.y.toDouble(), goal.z.toDouble())
            if (position.distanceSquared(goalPosition) < 4) {
                goalFuture = null
                future.complete(ContanVoidObject.INSTANCE)
            }
        }
    }

    /**
     * Update pathfinding.
     * @param locX entity locX
     * @param locY entity locY
     * @param locZ entity locZ
     */
    private fun updatePathfinding(locX: Int, locY: Int, locZ: Int) {
        val navigationGoal = this.navigationGoal ?: return
        val previousGoal = this.previousNavigationGoal
        if (previousGoal != null) {
            if (previousGoal == navigationGoal) {
                return
            }
        }
        this.previousNavigationGoal = navigationGoal

        if (!entity.isOnGround) {
            return
        }

        if (!isAsyncPathfinding) {
            val start = BlockPosition(locX, locY, locZ)
            pathfinder.start = start
            pathfinder.goal = navigationGoal
            val paths = pathfinder.runPathFinding()

            this.currentPaths = paths

            currentPathIndex = 0
            return
        }

        // Start AsyncAStarMachine
        val start = BlockPosition(locX, locY, locZ)
        pathfinder.start = start
        pathfinder.goal = navigationGoal
        pathfindingTask = pathfinder.runPathfindingAsync()

        // Then finish pathfinding
        pathfindingTask!!.thenAccept { paths ->
            if (paths.isEmpty()) {
                tickSyncTask = Runnable {
                    currentPaths = null
                    currentPathIndex = 0
                }
                return@thenAccept
            }

            tickSyncTask = Runnable {
                this.currentPaths = paths
                currentPathIndex = 1
            }
        }
    }

    fun setNavigationGoal(navigationGoal: BlockPosition?) {
        this.navigationGoal = navigationGoal
    }

    fun setNavigationGoalWithFuture(navigationGoal: BlockPosition?): ContanObject<*> {
        this.setNavigationGoal(navigationGoal)
        goalFuture = ContanUtil.createFutureInstance()
        return goalFuture!!.contanInstance
    }

    fun getNavigationGoal(): BlockPosition? {
        return navigationGoal
    }

    fun setAsync() {
        isAsyncPathfinding = true
        if (pathfindingInterval == 1) {
            setPathfindingInterval(2)
        }
    }

    fun setPathfindingInterval(interval: Int) {
        this.pathfindingInterval = interval
        this.tick += Random().nextInt(interval)
    }
}

