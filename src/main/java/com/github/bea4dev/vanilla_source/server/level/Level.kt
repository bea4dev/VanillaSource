package com.github.bea4dev.vanilla_source.server.level

import com.github.bea4dev.vanilla_source.Resources
import com.github.bea4dev.vanilla_source.config.server.LevelConfig
import com.github.bea4dev.vanilla_source.server.VanillaSource
import com.github.bea4dev.vanilla_source.server.level.entity.LevelEntityTypeRegistry
import com.github.bea4dev.vanilla_source.server.level.generator.GeneratorRegistry
import com.github.bea4dev.vanilla_source.util.asPosition
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import net.minestom.server.MinecraftServer
import net.minestom.server.event.instance.InstanceChunkUnloadEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.LightingChunk
import net.minestom.server.instance.anvil.AnvilLoader
import net.minestom.server.utils.NamespaceID
import net.minestom.server.utils.chunk.ChunkSupplier
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.util.*

class Level {
    companion object {

        private var levelNameMap = WeakHashMap<Instance, String>()
        private val levelEntitySettings = mutableMapOf<String, Pair<File, Map<String, Any>>>()

        @Synchronized
        private fun setLevelName(level: Instance, name: String) {
            // copy on write
            val clone = WeakHashMap(levelNameMap)
            clone[level] = name
            levelNameMap = clone
        }

        @Synchronized
        private fun getLevelEntitySettings(levelName: String): Pair<File, Map<String, Any>>? {
            val value = levelEntitySettings[levelName]
            return value?.let { Pair(it.first, (it.second)) }
        }

        @Synchronized
        private fun setLevelEntitySettings(levelName: String, settings: Pair<File, Map<String, Any>>) {
            levelEntitySettings[levelName] = settings
        }

        private fun saveLevelEntitySettings(levelName: String) {
            val settings = getLevelEntitySettings(levelName)
            settings?.let {
                FileWriter(it.first).use { writer -> Yaml().dump(settings.second, writer) }
            }
        }

        @JvmStatic
        fun load(name: String): Result<Instance, String> {
            var config: LevelConfig? = null
            for (levelConfig in VanillaSource.getServer().serverConfig.level.levels) {
                if (levelConfig.name == name) {
                    config = levelConfig
                    break
                }
            }

            return if (config == null) {
                Err("Level '${name}' is not found!")
            } else {
                load(config)
            }
        }

        @JvmStatic
        private fun load(levelConfig: LevelConfig): Result<Instance, String> {
            val instanceManager = MinecraftServer.getInstanceManager()
            val dimensionTypeName = levelConfig.dimensionType
            val dimensionRegistry = MinecraftServer.getDimensionTypeRegistry()
            val dimensionId = dimensionRegistry.getId(NamespaceID.from(dimensionTypeName))
            val dimension = dimensionRegistry.getKey(dimensionId)
                ?: return Err("[${levelConfig.name}] DimensionType '${dimensionTypeName}' is not found!")

            val level = instanceManager.createInstanceContainer(dimension)
            level.timeRate = 0

            val pathStr = levelConfig.path
            if (pathStr == null) {
                val generatorName = levelConfig.generator
                    ?: return Err("[${levelConfig.name}] Generator is required!")
                val generator = GeneratorRegistry.getGenerator(generatorName)
                    ?: return Err("[${levelConfig.name}] Generator '${generatorName}' is not found!")
                level.setGenerator(generator)

                /*
                if (generator is ChunkSupplier) {
                    level.chunkSupplier = generator
                }*/
            } else {
                level.chunkLoader = AnvilLoader(pathStr)

                val generatorName = levelConfig.generator
                if (generatorName != null) {
                    val generator = GeneratorRegistry.getGenerator(generatorName)
                        ?: return Err("[${levelConfig.name}] Generator '${generatorName}' is not found!")
                    level.setGenerator(generator)
                }
            }

            level.chunkSupplier =
                ChunkSupplier { instance: Instance, chunkX: Int, chunkZ: Int ->
                    LightingChunk(
                        instance,
                        chunkX,
                        chunkZ
                    )
                }

            if (levelConfig.save) {
                if (pathStr == null) {
                    return Err("[${levelConfig.name}] Specify the path of the level to enable auto save!")
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

            val levelEntityConfigFile = File(levelConfig.entities)
            if (!levelEntityConfigFile.exists()) {
                if (levelConfig.entities == "level_entities/debug_level_entities.yml") {
                    // if default file
                    File("level_entities").mkdir()
                    Resources.saveResource("level_entities/debug_level_entities.yml", false)
                } else {
                    return Err("Level entity settings '${levelConfig.entities}' is not found!")
                }
            }

            val levelEntityMap: Map<String, Any> = Yaml().load(FileInputStream(levelEntityConfigFile))
            @Suppress("UNCHECKED_CAST")
            for (entry in levelEntityMap.entries) {
                val entityName = entry.key
                val settings = entry.value as Map<String, Any>
                val typeName = settings["type"]!! as String
                val position = settings["position"]?.let { it as Map<String, Any> }?.asPosition()

                val levelEntity = LevelEntityTypeRegistry.INSTANCE[typeName]
                    ?: return Err("Level entity '$typeName' is not found!")

                levelEntity.createEntity(entityName, levelConfig.name, level, position, settings)
            }
            setLevelEntitySettings(levelConfig.name, Pair(levelEntityConfigFile, levelEntityMap))

            return Ok(level)
        }
    }
}