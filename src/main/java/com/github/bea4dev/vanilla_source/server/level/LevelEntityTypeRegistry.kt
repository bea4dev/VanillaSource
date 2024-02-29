package com.github.bea4dev.vanilla_source.server.level

import com.github.bea4dev.vanilla_source.resource.FreezableResource

class LevelEntityTypeRegistry: FreezableResource() {

    companion object {
        @JvmStatic
        val INSTANCE = LevelEntityTypeRegistry()

        fun freezeRegistry() {
            INSTANCE.freeze()
        }

    }

    private val levelEntityTypeMap = mutableMapOf<String, LevelEntityType>()

    operator fun get(typeName: String): LevelEntityType? {
        return levelEntityTypeMap[typeName]
    }

    operator fun set(typeName: String, levelEntityType: LevelEntityType) {
        assert()
        levelEntityTypeMap[typeName] = levelEntityType
    }

}