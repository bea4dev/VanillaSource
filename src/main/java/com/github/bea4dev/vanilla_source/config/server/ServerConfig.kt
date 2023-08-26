package com.github.bea4dev.vanilla_source.config.server

import cc.ekblad.toml.TomlMapper
import cc.ekblad.toml.tomlMapper
import com.github.bea4dev.vanilla_source.config.DefaultTomlConfig
import com.github.bea4dev.vanilla_source.config.TomlConfig
import com.github.bea4dev.vanilla_source.config.types.Position

data class ServerConfig(

    val address: AddressAndPort,
    val level: LevelConfigs,

) : TomlConfig {

    companion object: DefaultTomlConfig {
        override fun default(): TomlConfig {
            return ServerConfig(
                AddressAndPort("0.0.0.0", 25565),
                LevelConfigs(
                    DefaultLevel("debug", Position(0.0, 66.0, 0.0, null, null)),
                    listOf(LevelConfig("debug", null, "debug", "overworld", false)),
                ),
            )
        }

        override fun mapper(): TomlMapper {
            return tomlMapper {
                mapping<DefaultLevel>("spawn_position" to "spawnPosition")
                mapping<LevelConfig>("dimension_type" to "dimensionType")
            }
        }
    }

}

data class AddressAndPort(val ip: String, val port: Int)

data class LevelConfigs(val default: DefaultLevel, val levels: List<LevelConfig>)

data class DefaultLevel(val name: String, val spawnPosition: Position)

data class LevelConfig(
    val name: String,
    val path: String?,
    val generator: String?,
    val dimensionType: String,
    val save: Boolean,
)
