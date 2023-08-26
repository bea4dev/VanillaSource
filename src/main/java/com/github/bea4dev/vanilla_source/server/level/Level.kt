package com.github.bea4dev.vanilla_source.server.level

import com.github.bea4dev.vanilla_source.config.server.LevelConfig
import com.github.bea4dev.vanilla_source.server.VanillaSource
import com.github.bea4dev.vanilla_source.server.level.generator.GeneratorRegistry
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import net.minestom.server.MinecraftServer
import net.minestom.server.event.instance.InstanceChunkLoadEvent
import net.minestom.server.event.instance.InstanceChunkUnloadEvent
import net.minestom.server.instance.AnvilLoader
import net.minestom.server.instance.Instance
import net.minestom.server.utils.NamespaceID
import net.minestom.server.utils.chunk.ChunkSupplier
import java.util.concurrent.ConcurrentHashMap

class Level {
    companion object {
        private val levelMap = ConcurrentHashMap<String, Instance>()

        @JvmStatic
        fun getLevel(name: String): Instance? {
            return levelMap[name]
        }

        @JvmStatic
        fun register(name: String, level: Instance) {
            levelMap[name] = level
        }

        @JvmStatic
        fun unregister(name: String) {
            levelMap.remove(name)
        }

        @JvmStatic
        fun levels(): Collection<Instance> {
            return levelMap.values
        }

        @JvmStatic
        fun load(name: String): Result<Instance, Throwable> {
            var config: LevelConfig? = null
            for (levelConfig in VanillaSource.getServer().serverConfig.level.levels) {
                if (levelConfig.name == name) {
                    config = levelConfig
                    break
                }
            }

            return if (config == null) {
                Err(IllegalStateException("Level '${name}' is not found!"))
            } else {
                load(config)
            }
        }

        @JvmStatic
        fun load(levelConfig: LevelConfig): Result<Instance, Throwable> {
            val instanceManager = MinecraftServer.getInstanceManager()
            val dimensionTypeName = levelConfig.dimensionType
            val dimension = MinecraftServer.getDimensionTypeManager().getDimension(NamespaceID.from(dimensionTypeName))
                ?: return Err(IllegalArgumentException("[${levelConfig.name}] DimensionType '${dimensionTypeName}' is not found!"))

            val level = instanceManager.createInstanceContainer(dimension)

            val pathStr = levelConfig.path
            if (pathStr == null) {
                val generatorName = levelConfig.generator
                    ?: return Err(IllegalArgumentException("[${levelConfig.name}] Generator is required!"))
                val generator = GeneratorRegistry.getGenerator(generatorName)
                    ?: return Err(IllegalArgumentException("[${levelConfig.name}] Generator '${generatorName}' is not found!"))
                level.setGenerator(generator)

                if (generator is ChunkSupplier) {
                    level.chunkSupplier = generator
                }
            } else {
                level.chunkLoader = AnvilLoader(pathStr)

                val generatorName = levelConfig.generator
                if (generatorName != null) {
                    val generator = GeneratorRegistry.getGenerator(generatorName)
                        ?: return Err(IllegalArgumentException("[${levelConfig.name}] Generator '${generatorName}' is not found!"))
                    level.setGenerator(generator)
                }
            }

            if (levelConfig.save) {
                if (pathStr == null) {
                    return Err(IllegalArgumentException("[${levelConfig.name}] Specify the path of the level to enable auto save!"))
                }

                // On chunk unload
                val handler = MinecraftServer.getGlobalEventHandler()
                handler.addListener(InstanceChunkUnloadEvent::class.java) { event ->
                    val chunk = event.chunk
                    level.chunkLoader.saveChunk(chunk)
                }

                // On shutdown
                MinecraftServer.getSchedulerManager().buildShutdownTask {
                    VanillaSource.getServer().logger.info("Saving level '${levelConfig.name}'...")
                    level.saveInstance()
                }
            }

            register(levelConfig.name, level)

            return Ok(level)
        }
    }
}