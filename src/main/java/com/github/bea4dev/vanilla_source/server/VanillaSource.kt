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
import com.github.bea4dev.vanilla_source.server.entity.ai.astar.AsyncPathfinderThread
import com.github.bea4dev.vanilla_source.server.item.ItemRegistry
import com.github.bea4dev.vanilla_source.server.level.DebugLevelEntityType
import com.github.bea4dev.vanilla_source.server.level.Level
import com.github.bea4dev.vanilla_source.server.level.LevelChunkThreadProvider
import com.github.bea4dev.vanilla_source.server.level.LevelEntityTypeRegistry
import com.github.bea4dev.vanilla_source.server.level.generator.GeneratorRegistry
import com.github.bea4dev.vanilla_source.server.listener.registerItemListener
import com.github.bea4dev.vanilla_source.server.player.VanillaSourcePlayerProvider
import com.github.michaelbull.result.unwrap
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.*
import net.minestom.server.entity.fakeplayer.FakePlayer
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.extras.MojangAuth
import net.minestom.server.instance.Instance
import net.minestom.server.thread.ThreadDispatcher
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean


@Suppress("UnstableApiUsage")
class VanillaSource(val serverConfig: ServerConfig, private val console: Console?) {
    private val minecraftServer: MinecraftServer = MinecraftServer.initWithCustomDispatcher(
        ThreadDispatcher.of(
            LevelChunkThreadProvider(serverConfig.settings.maxWorldTickThreads),
            serverConfig.settings.maxWorldTickThreads
        )
    )

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

        // Register events
        registerItemListener()

        /*
        MinecraftServer.getGlobalEventHandler().addListener(PlayerStartSneakingEvent::class.java) { event ->
            val player = event.player

            val zombie = TestZombie()
            zombie.getAttribute(Attribute.MOVEMENT_SPEED).baseValue = 0.16F
            zombie.isAutoViewable = true
            zombie.aiController.goalSelector.goals += EntityTargetAttackGoal(zombie, player, 2.5, 5)
            zombie.setNoGravity(false)
            //zombie.setInstance(player.instance, player.position)
        }*/
        MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent::class.java) { event ->
            if (event.player is FakePlayer) {
                return@addListener
            }
            event.player.permissionLevel = 2
        }

        // Register player object provider
        MinecraftServer.getConnectionManager().setPlayerProvider(VanillaSourcePlayerProvider())

        // Register debug level entity
        LevelEntityTypeRegistry.INSTANCE["debug_entity"] = DebugLevelEntityType()
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
            defaultLevel = Level.load(defaultLevelConfig.name).unwrap()

            // Set default spawn position
            val globalEventHandler = MinecraftServer.getGlobalEventHandler()
            globalEventHandler.addListener(AsyncPlayerConfigurationEvent::class.java) { event ->
                val player: Player = event.player
                event.spawningInstance = defaultLevel!!
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

            // enable mojang auth
            MojangAuth.init()

            freezeRegistries()

            // Runs the garbage collector before starting.
            System.gc()

            // Start
            val address = serverConfig.address
            this.minecraftServer.start(address.ip, address.port)

            logger.info("Hello, Minecraft!")
        }
    }

    private fun freezeRegistries() {
        ItemRegistry.freezeRegistry()
        LevelEntityTypeRegistry.freezeRegistry()
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