package com.github.bea4dev.vanilla_source.server

import com.github.bea4dev.vanilla_source.Console
import com.github.bea4dev.vanilla_source.commands.Commands
import com.github.bea4dev.vanilla_source.config.server.ServerConfig
import com.github.bea4dev.vanilla_source.logger.STDOutLogger
import com.github.bea4dev.vanilla_source.server.level.Level
import com.github.bea4dev.vanilla_source.server.level.generator.GeneratorRegistry
import com.github.bea4dev.vanilla_source.util.unwrap
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.event.player.PlayerLoginEvent
import net.minestom.server.instance.Instance
import org.slf4j.LoggerFactory
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
    }

    init {
        MinecraftServer.setBrandName("VanillaSource")
        server = this
    }

    private fun registerTask() {
        GeneratorRegistry.init()
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