package com.github.bea4dev.vanilla_source.server

import com.github.bea4dev.vanilla_source.Console
import com.github.bea4dev.vanilla_source.commands.Commands
import com.github.bea4dev.vanilla_source.config.server.ServerConfig
import com.github.bea4dev.vanilla_source.lang.LanguageText
import com.github.bea4dev.vanilla_source.lang.TranslateRenderer
import com.github.bea4dev.vanilla_source.logger.STDOutLogger
import com.github.bea4dev.vanilla_source.plugin.PluginManager
import com.github.bea4dev.vanilla_source.resource.model.EntityModelResources
import com.github.bea4dev.vanilla_source.server.debug.registerBenchmarkTask
import com.github.bea4dev.vanilla_source.server.item.ItemRegistry
import com.github.bea4dev.vanilla_source.server.level.Level
import com.github.bea4dev.vanilla_source.server.level.entity.DebugLevelEntityType
import com.github.bea4dev.vanilla_source.server.level.entity.LevelEntityTypeRegistry
import com.github.bea4dev.vanilla_source.server.level.generator.GeneratorRegistry
import com.github.bea4dev.vanilla_source.server.level.registerDimensions
import com.github.bea4dev.vanilla_source.server.listener.registerBlockListener
import com.github.bea4dev.vanilla_source.server.listener.registerEntityListener
import com.github.bea4dev.vanilla_source.server.listener.registerItemListener
import com.github.bea4dev.vanilla_source.server.player.VanillaSourcePlayerProvider
import com.github.bea4dev.vanilla_source.server.player.registerPlayerEventListener
import com.github.michaelbull.result.unwrap
import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.adventure.MinestomAdventure
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.extras.MojangAuth
import net.minestom.server.instance.Instance
import net.minestom.server.instance.InstanceThreadProvider
import net.minestom.server.thread.ThreadDispatcher
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.BiFunction
import kotlin.system.exitProcess


@Suppress("UnstableApiUsage")
class VanillaSource(val serverConfig: ServerConfig, private val console: Console?) {
    private val minecraftServer: MinecraftServer = MinecraftServer.initWithDispatcher(
        ThreadDispatcher.of(
            InstanceThreadProvider(),
            serverConfig.settings.maxWorldTickThreads
        )
    )

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

        // Entity models
        if (serverConfig.settings.enableModelEngine) {
            EntityModelResources.loadModels()
        }

        // Register events
        registerItemListener()
        registerBlockListener()
        registerEntityListener()

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
            event.player.permissionLevel = 2
        }

        // Register player object provider
        MinecraftServer.getConnectionManager().setPlayerProvider(VanillaSourcePlayerProvider())

        // Register debug level entity type
        LevelEntityTypeRegistry.INSTANCE["debug_entity"] = DebugLevelEntityType()

        registerEventListeners()

        registerDimensions()
    }

    private fun registerEventListeners() {
        registerPlayerEventListener()
    }

    @Suppress("DEPRECATION")
    fun start() {
        isRunning.set(true)

        task {
            logger.info("Starting...")

            // Log setting
            System.setOut(STDOutLogger(System.out))
            System.setErr(STDOutLogger(System.err))

            // Load text
            LanguageText.initialize()

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

            // Register shutdown task
            MinecraftServer.getSchedulerManager().buildShutdownTask {
                logger.info("Good night.")
            }

            // Register commands
            Commands.register()

            // enable mojang auth
            MojangAuth.init()

            freezeRegistries()

            // Enable translation
            MinestomAdventure.AUTOMATIC_COMPONENT_TRANSLATION = true
            MinestomAdventure.COMPONENT_TRANSLATOR = BiFunction { component: Component?, locale: Locale ->
                component?.let { TranslateRenderer.render(component, locale) }
            }

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
            pluginManager.onDisable()
            console?.stop()
            exitProcess(0)
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