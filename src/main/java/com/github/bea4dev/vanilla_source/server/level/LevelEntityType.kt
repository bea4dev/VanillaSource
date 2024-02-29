package com.github.bea4dev.vanilla_source.server.level

import net.minestom.server.coordinate.Pos
import net.minestom.server.instance.Instance

interface LevelEntityType {

    fun createEntity(
        entityName: String,
        levelName: String,
        level: Instance,
        position: Pos?,
        settings: Map<String, Any>
    )

}