package com.github.bea4dev.vanilla_source.config.resource

import cc.ekblad.toml.TomlMapper
import cc.ekblad.toml.tomlMapper
import com.github.bea4dev.vanilla_source.config.DefaultTomlConfig
import com.github.bea4dev.vanilla_source.config.TomlConfig

data class EntityModelConfig(val settings: EntityModelSetting) : TomlConfig {

    companion object : DefaultTomlConfig {
        override fun default(): TomlConfig {
            return EntityModelConfig(EntityModelSetting(
                "resource-pack-template",
                "bbmodels",
                "resource-pack"
            ))
        }

        override fun mapper(): TomlMapper {
            return tomlMapper {
                mapping<EntityModelSetting>(
                    "template_path" to "templatePath",
                    "bbmodels_path" to "bbmodelsPath",
                    "output_name"   to "outputName"
                )
            }
        }

    }

}

data class EntityModelSetting(
    val templatePath: String,
    val bbmodelsPath: String,
    val outputName: String
)