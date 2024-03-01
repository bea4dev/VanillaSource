package com.github.bea4dev.vanilla_source.config.server

import cc.ekblad.toml.TomlMapper
import cc.ekblad.toml.tomlMapper
import com.github.bea4dev.vanilla_source.config.DefaultTomlConfig
import com.github.bea4dev.vanilla_source.config.TomlConfig
import com.github.bea4dev.vanilla_source.config.types.Position

data class ServerConfig(
    val address: AddressAndPort,
    val settings: ServerSettings,
    val level: LevelConfigs,
) : TomlConfig {

    companion object: DefaultTomlConfig {
        override fun default(): TomlConfig {
            return ServerConfig(
                AddressAndPort("0.0.0.0", 25565),
                ServerSettings(false, 4, 4, 12, 8),
                LevelConfigs(
                    DefaultLevel("debug", "CREATIVE", Position(0.0, 66.0, 0.0, null, null)),
                    listOf(LevelConfig(
                        "debug",
                        null,
                        "debug",
                        "overworld",
                        false,
                        "level_entities/debug_level_entities.yml"
                    ))
                )
            )
        }

        override fun mapper(): TomlMapper {
            return tomlMapper {
                mapping<ServerSettings>(
                    "enable_model_engine"        to "enableModelEngine",
                    "max_world_tick_threads"     to "maxWorldTickThreads",
                    "async_pathfinding_threads"  to "asyncPathfindingThreads",
                    "chunk_view_distance"        to "chunkViewDistance",
                    "entity_view_distance"       to "entityViewDistance"
                )
                mapping<DefaultLevel>("spawn_position" to "spawnPosition", "game_mode" to "gameMode")
                mapping<LevelConfig>("dimension_type" to "dimensionType")
            }
        }
    }

}

data class ServerSettings(
    val enableModelEngine: Boolean,
    val maxWorldTickThreads: Int,
    val asyncPathfindingThreads: Int,
    val chunkViewDistance: Int,
    val entityViewDistance: Int
)

data class AddressAndPort(val ip: String, val port: Int)

data class LevelConfigs(val default: DefaultLevel, val levels: List<LevelConfig>)

data class DefaultLevel(val name: String, val gameMode: String?, val spawnPosition: Position)

data class LevelConfig(
    val name: String,
    val path: String?,
    val generator: String?,
    val dimensionType: String,
    val save: Boolean,
    val entities: String
)
