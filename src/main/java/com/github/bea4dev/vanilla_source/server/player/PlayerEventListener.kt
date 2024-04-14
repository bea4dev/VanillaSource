package com.github.bea4dev.vanilla_source.server.player

import com.github.bea4dev.vanilla_source.hud.LinedSidebar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.event.inventory.InventoryCloseEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.timer.TaskSchedule

fun registerPlayerEventListener() {
    playerInventoryListener()

    MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent::class.java) { event ->
        val sidebar = LinedSidebar(Component.translatable("gui.main_menu"), 5)
        sidebar.addViewer(event.player)

        var i = 0
        MinecraftServer.getSchedulerManager().scheduleTask({
            sidebar.setLine(0, Component.translatable("gui.main_menu").append(Component.text(" 0 : $i")))
            sidebar.setLine(1, Component.translatable("gui.main_menu").append(Component.text(" 1 : $i")))
            i++
        }, TaskSchedule.immediate(), TaskSchedule.tick(20))
    }
}

fun playerInventoryListener() {
    MinecraftServer.getGlobalEventHandler().addListener(InventoryCloseEvent::class.java) { event ->
        val player = event.player as VanillaSourcePlayer
        val history = player.guiHistory
        synchronized(history) { history.clear() }
    }
}