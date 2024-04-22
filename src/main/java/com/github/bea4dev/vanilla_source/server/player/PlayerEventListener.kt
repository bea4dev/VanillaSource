package com.github.bea4dev.vanilla_source.server.player

import com.github.bea4dev.vanilla_source.camera.Camera
import net.minestom.server.MinecraftServer
import net.minestom.server.event.inventory.InventoryCloseEvent
import net.minestom.server.event.player.PlayerHandAnimationEvent
import net.minestom.server.timer.TaskSchedule

fun registerPlayerEventListener() {
    playerInventoryListener()

    MinecraftServer.getGlobalEventHandler().addListener(PlayerHandAnimationEvent::class.java) { event ->
        val player = event.player as VanillaSourcePlayer
        val camera = Camera(player.position)
        camera.addPLayer(player)

        MinecraftServer.getSchedulerManager().scheduleTask({
            camera.teleport(camera.position.add(0.0, 0.1, 0.1).withYaw(camera.position.yaw + 0.1F))
        }, TaskSchedule.immediate(), TaskSchedule.tick(1))
    }
}

fun playerInventoryListener() {
    MinecraftServer.getGlobalEventHandler().addListener(InventoryCloseEvent::class.java) { event ->
        val player = event.player as VanillaSourcePlayer
        val history = player.guiHistory
        synchronized(history) { history.clear() }
    }
}