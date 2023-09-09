package com.github.bea4dev.vanilla_source.server

import com.github.bea4dev.vanilla_source.Console
import com.github.bea4dev.vanilla_source.Resources
import com.github.bea4dev.vanilla_source.commands.Commands
import com.github.bea4dev.vanilla_source.config.TomlConfig
import com.github.bea4dev.vanilla_source.config.resource.EntityModelConfig
import com.github.bea4dev.vanilla_source.config.server.ServerConfig
import com.github.bea4dev.vanilla_source.logger.STDOutLogger
import com.github.bea4dev.vanilla_source.resource.model.EntityModelResource
import com.github.bea4dev.vanilla_source.server.entity.EnemyModelEntity
import com.github.bea4dev.vanilla_source.server.entity.ai.goal.EntityTargetGoal
import com.github.bea4dev.vanilla_source.server.item.ItemRegistry
import com.github.bea4dev.vanilla_source.server.item.VanillaSourceItem
import com.github.bea4dev.vanilla_source.server.level.Level
import com.github.bea4dev.vanilla_source.server.level.generator.GeneratorRegistry
import com.github.bea4dev.vanilla_source.test.TestZombie
import com.github.bea4dev.vanilla_source.util.unwrap
import net.minestom.server.MinecraftServer
import net.minestom.server.attribute.Attribute
import net.minestom.server.entity.*
import net.minestom.server.entity.ai.goal.MeleeAttackGoal
import net.minestom.server.entity.ai.goal.RandomStrollGoal
import net.minestom.server.entity.ai.target.ClosestEntityTarget
import net.minestom.server.entity.ai.target.LastEntityDamagerTarget
import net.minestom.server.entity.metadata.display.ItemDisplayMeta
import net.minestom.server.event.entity.EntityAttackEvent
import net.minestom.server.event.player.PlayerLoginEvent
import net.minestom.server.event.player.PlayerStartSneakingEvent
import net.minestom.server.instance.Instance
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket
import net.minestom.server.network.packet.server.play.EntityPositionPacket
import net.minestom.server.network.packet.server.play.EntityTeleportPacket
import net.minestom.server.network.packet.server.play.SpawnEntityPacket
import net.minestom.server.timer.TaskSchedule
import net.minestom.server.utils.time.TimeUnit
import org.slf4j.LoggerFactory
import team.unnamed.hephaestus.minestom.MinestomModelEngine
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean


class VanillaSource(val serverConfig: ServerConfig, private val console: Console?) {

    private val minecraftServer: MinecraftServer = MinecraftServer.init()
    val logger = LoggerFactory.getLogger("VanillaSource")!!
    private var defaultLevel: Instance? = null
    private val isRunning = AtomicBoolean(false)

    companion object {
        private var server: VanillaSource? = null

        fun getServer(): VanillaSource {
            return server!!
        }

        fun getMinecraftServer(): MinecraftServer {
            return server!!.minecraftServer
        }
    }

    init {
        MinecraftServer.setBrandName("VanillaSource")
        server = this
    }

    private fun registerTask() {
        GeneratorRegistry.init()

        // Entity models
        if (serverConfig.settings.enableModelEngine) {
            Resources.saveResource("entity_model.toml", false)
            val entityModelConfig = TomlConfig.loadOrDefault<EntityModelConfig>(File("entity_model.toml")).unwrap()
            EntityModelResource.createGlobalResource(entityModelConfig)
        }

        // Freeze item registry
        ItemRegistry.init()
        ItemRegistry.freezeRegistry()

        // Register events
        MinecraftServer.getGlobalEventHandler().addListener(EntityAttackEvent::class.java) { event ->
            val target = event.target
            val source = event.entity

            if (source is Player) {
                val itemStack = source.itemInMainHand
                val id = itemStack.meta().getTag(VanillaSourceItem.idTag) ?: return@addListener

                val item = ItemRegistry.INSTANCE[id]
                if (item == null) {
                    logger.warn("Unknown item id '${id}'!")
                    return@addListener
                }
                if (target is EnemyModelEntity) {
                    target.onAttacked(source, item)
                }
                item.onAttack(source, target, itemStack)
            }
        }
        MinecraftServer.getGlobalEventHandler().addListener(PlayerStartSneakingEvent::class.java) { event ->
            val player = event.player
            val zombie = TestZombie()
            zombie.getAttribute(Attribute.MOVEMENT_SPEED).baseValue = 0.2F
            zombie.isAutoViewable = true
            zombie.aiController.goalSelector.goals += EntityTargetGoal(player)
            zombie.setNoGravity(false)
            zombie.setInstance(player.instance, player.position)
        }
        MinecraftServer.getGlobalEventHandler().addListener(PlayerLoginEvent::class.java) { event ->
            event.player.permissionLevel = 2
            val item = ItemRegistry.INSTANCE["pipe"]!!
            event.player.inventory.addItemStack(item.createItemStack().withMeta { builder -> builder.customModelData(1) })
        }
    }

    @Suppress("DEPRECATION")
    fun start() {
        isRunning.set(true)

        task {
            logger.info("Starting...")

            // Log setting
            System.setOut(STDOutLogger(System.out))
            System.setErr(STDOutLogger(System.err))

            registerTask()

            // Load all levels
            val defaultLevelConfig = serverConfig.level.default
            for (levelConfig in serverConfig.level.levels) {
                logger.info("Loading level '${levelConfig.name}'...")
                val level = Level.load(levelConfig).unwrap()
                if (levelConfig.name == defaultLevelConfig.name) {
                    defaultLevel = level
                }
            }
            if (defaultLevel == null) {
                throw Exception("Level '${defaultLevelConfig.name}' is not found!")
            }

            // Set default spawn position
            val globalEventHandler = MinecraftServer.getGlobalEventHandler()
            globalEventHandler.addListener(PlayerLoginEvent::class.java) { event ->
                val player: Player = event.player
                event.setSpawningInstance(defaultLevel!!)
                player.respawnPoint = defaultLevelConfig.spawnPosition.toPos()
                // Set default game mode
                val defaultGameMode = serverConfig.level.default.gameMode
                if (defaultGameMode != null) {
                    val gameMode = GameMode.valueOf(defaultGameMode)
                    player.gameMode = gameMode
                }
            }

            // Apply server settings
            MinecraftServer.setChunkViewDistance(serverConfig.settings.chunkViewDistance)
            MinecraftServer.setEntityViewDistance(serverConfig.settings.entityViewDistance)

            // Register shutdown task
            MinecraftServer.getSchedulerManager().buildShutdownTask {
                logger.info("Good night.")
            }

            // Register commands
            Commands.register()

            // Runs the garbage collection before starting.
            System.gc()

            // Start
            val address = serverConfig.address
            this.minecraftServer.start(address.ip, address.port)

            logger.info("Hello, Minecraft!")
        }
    }

    private fun <T> task(task: () -> T) {
        try {
            task()
        } catch (error: Throwable) {
            stopWithFatalError(error)
        }
    }

    fun stop() {
        if (isRunning.getAndSet(false)) {
            MinecraftServer.stopCleanly()
            console?.stop()
        }
    }

    fun stopWithFatalError(error: Throwable) {
        if (isRunning.getAndSet(false)) {
            logger.warn("!!! SERVER IS STOPPED WITH FATAL ERROR !!!")
            error.printStackTrace()
            MinecraftServer.stopCleanly()
            console?.stop()
        }
    }


    fun getDefaultLevel(): Instance {
        return this.defaultLevel!!
    }

}