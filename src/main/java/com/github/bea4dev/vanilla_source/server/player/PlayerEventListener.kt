package com.github.bea4dev.vanilla_source.server.player

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.event.inventory.InventoryCloseEvent
import net.minestom.server.event.player.PlayerSpawnEvent

fun registerPlayerEventListener() {
    playerInventoryListener()
}

fun playerInventoryListener() {
    MinecraftServer.getGlobalEventHandler().addListener(InventoryCloseEvent::class.java) { event ->
        val player = event.player as VanillaSourcePlayer
        val history = player.guiHistory
        synchronized(history) { history.clear() }
    }
}