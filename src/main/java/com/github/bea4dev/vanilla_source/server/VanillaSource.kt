package com.github.bea4dev.vanilla_source.server

import com.github.bea4dev.vanilla_source.Console
import com.github.bea4dev.vanilla_source.Resources
import com.github.bea4dev.vanilla_source.commands.Commands
import com.github.bea4dev.vanilla_source.config.TomlConfig
import com.github.bea4dev.vanilla_source.config.resource.EntityModelConfig
import com.github.bea4dev.vanilla_source.config.server.ServerConfig
import com.github.bea4dev.vanilla_source.logger.STDOutLogger
import com.github.bea4dev.vanilla_source.natives.NativeManager
import com.github.bea4dev.vanilla_source.natives.registerNativeChunkListener
import com.github.bea4dev.vanilla_source.plugin.PluginManager
import com.github.bea4dev.vanilla_source.resource.model.EntityModelResource
import com.github.bea4dev.vanilla_source.server.debug.registerBenchmarkTask
import com.github.bea4dev.vanilla_source.server.entity.ai.EntityAIController
import com.github.bea4dev.vanilla_source.server.entity.ai.astar.AsyncPathfinderThread
import com.github.bea4dev.vanilla_source.server.entity.ai.goal.EntityFollowGoal
import com.github.bea4dev.vanilla_source.server.entity.ai.goal.EntityTargetAttackGoal
import com.github.bea4dev.vanilla_source.server.item.ItemRegistry
import com.github.bea4dev.vanilla_source.server.level.Level
import com.github.bea4dev.vanilla_source.server.level.LevelChunkThreadProvider
import com.github.bea4dev.vanilla_source.server.level.generator.GeneratorRegistry
import com.github.bea4dev.vanilla_source.server.listener.registerEntityAttackListener
import com.github.bea4dev.vanilla_source.server.player.VanillaSourcePlayerProvider
import com.github.bea4dev.vanilla_source.test.TestZombie
import com.github.bea4dev.vanilla_source.util.unwrap
import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.attribute.Attribute
import net.minestom.server.entity.*
import net.minestom.server.entity.fakeplayer.FakePlayer
import net.minestom.server.entity.fakeplayer.FakePlayerOption
import net.minestom.server.event.player.PlayerChatEvent
import net.minestom.server.event.player.PlayerLoginEvent
import net.minestom.server.event.player.PlayerStartSneakingEvent
import net.minestom.server.instance.Instance
import net.minestom.server.network.packet.server.play.PlayerInfoRemovePacket
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket
import net.minestom.server.thread.ThreadDispatcher
import net.minestom.server.timer.Task
import net.minestom.server.timer.TaskSchedule
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger


@Suppress("UnstableApiUsage")
class VanillaSource(val serverConfig: ServerConfig, private val console: Console?) {
    private val minecraftServer: MinecraftServer = MinecraftServer.initWithCustomDispatcher(
        ThreadDispatcher.of(LevelChunkThreadProvider(serverConfig.settings.maxWorldTickThreads), serverConfig.settings.maxWorldTickThreads))

    val nativeManager = NativeManager()
    val logger = LoggerFactory.getLogger("VanillaSource")!!
    private var defaultLevel: Instance? = null
    private val isRunning = AtomicBoolean(false)
    private val pluginManager = PluginManager()

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

        // For native libs
        registerNativeChunkListener()

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
        registerEntityAttackListener()

        MinecraftServer.getGlobalEventHandler().addListener(PlayerStartSneakingEvent::class.java) { event ->
            val player = event.player

            val zombie = TestZombie()
            zombie.getAttribute(Attribute.MOVEMENT_SPEED).baseValue = 0.16F
            zombie.isAutoViewable = true
            zombie.aiController.goalSelector.goals += EntityTargetAttackGoal(zombie, player, 2.5, 5)
            zombie.setNoGravity(false)
            //zombie.setInstance(player.instance, player.position)
        }
        MinecraftServer.getGlobalEventHandler().addListener(PlayerLoginEvent::class.java) { event ->
            if (event.player is FakePlayer) {
                return@addListener
            }
            event.player.permissionLevel = 2
            val item = ItemRegistry.INSTANCE["pipe"]!!
            event.player.inventory.addItemStack(item.createItemStack().withMeta { builder -> builder.customModelData(1) })
        }

        // Register player object provider
        MinecraftServer.getConnectionManager().setPlayerProvider(VanillaSourcePlayerProvider())
    }

    @Suppress("DEPRECATION")
    fun start() {
        isRunning.set(true)

        task {
            logger.info("Starting...")

            // Log setting
            System.setOut(STDOutLogger(System.out))
            System.setErr(STDOutLogger(System.err))

            // Load native libs
            nativeManager.init()

            registerTask()
            registerBenchmarkTask()

            // Load plugins
            pluginManager.onEnable()

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

            // Start async pathfinder threads
            AsyncPathfinderThread.initialize(serverConfig.settings.asyncPathfindingThreads)

            // Register shutdown task
            MinecraftServer.getSchedulerManager().buildShutdownTask {
                logger.info("Good night.")
            }

            // Register commands
            Commands.register()

            // Runs the garbage collector before starting.
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
            AsyncPathfinderThread.shutdownAll()
            pluginManager.onDisable()
            console?.stop()
        }
    }

    fun stopWithFatalError(error: Throwable) {
        if (isRunning.getAndSet(false)) {
            logger.warn("!!! SERVER IS STOPPED WITH FATAL ERROR !!!")
            error.printStackTrace()
            MinecraftServer.stopCleanly()
            AsyncPathfinderThread.shutdownAll()
            console?.stop()
        }
    }


    fun getDefaultLevel(): Instance {
        return this.defaultLevel!!
    }

}