package com.github.bea4dev.vanilla_source.server.level.entity

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.instance.Instance

@Suppress("UnstableApiUsage")
class DebugLevelEntityType: LevelEntityType {
    override fun createEntity(
        entityName: String,
        levelName: String,
        level: Instance,
        position: Pos?,
        settings: Map<String, Any>
    ) {
        val testString = settings["test_string"]!! as String

        MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent::class.java) { event ->
            if (event.instance != level) {
                return@addListener
            }
            event.player.sendMessage(testString)
        }
    }

}