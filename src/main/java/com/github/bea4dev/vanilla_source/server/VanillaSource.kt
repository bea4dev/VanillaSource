package com.github.bea4dev.vanilla_source.server

import com.github.bea4dev.vanilla_source.config.server.ServerConfig
import com.github.bea4dev.vanilla_source.server.level.Level
import com.github.bea4dev.vanilla_source.server.level.generator.GeneratorRegistry
import com.github.bea4dev.vanilla_source.util.unwrap
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.event.player.PlayerLoginEvent
import net.minestom.server.instance.Instance
import org.slf4j.LoggerFactory
import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4J


class VanillaSource(val serverConfig: ServerConfig) {

    private val minecraftServer: MinecraftServer = MinecraftServer.init()
    val logger = LoggerFactory.getLogger("VanillaSource")!!
    private var defaultLevel: Instance? = null

    companion object {
        private var server: VanillaSource? = null

        fun getServer(): VanillaSource { return server!! }
    }

    init {
        MinecraftServer.setBrandName("VanillaSource")
        server = this
    }

    private fun registerTask() {
        GeneratorRegistry.init()
    }

    fun start() {
        task {
            logger.info("Starting...")

            // Log setting
            SysOutOverSLF4J.sendSystemOutAndErrToSLF4J()

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
            }

            // Register shutdown task
            MinecraftServer.getSchedulerManager().buildShutdownTask {
                logger.info("Good night...")
            }

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
        MinecraftServer.stopCleanly()
    }

    fun stopWithFatalError(error: Throwable) {
        logger.warn("!!! SERVER IS STOPPED WITH FATAL ERROR !!!")
        error.printStackTrace()
        MinecraftServer.stopCleanly()
    }



    fun getDefaultLevel(): Instance { return this.defaultLevel!! }

}