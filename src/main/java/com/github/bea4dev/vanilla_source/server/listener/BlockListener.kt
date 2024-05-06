package com.github.bea4dev.vanilla_source.server.listener

import com.github.bea4dev.vanilla_source.server.level.block.DiggingHandler
import com.github.bea4dev.vanilla_source.server.level.block.GLOBAL_DIGGING_HANDLER
import com.github.bea4dev.vanilla_source.server.player.VanillaSourcePlayer
import net.minestom.server.MinecraftServer
import net.minestom.server.event.player.PlayerCancelDiggingEvent
import net.minestom.server.event.player.PlayerFinishDiggingEvent
import net.minestom.server.event.player.PlayerStartDiggingEvent

fun registerBlockListener() {
    playerDiggingListener()
}


@Suppress("UnstableApiUsage")
private fun playerDiggingListener() {
    MinecraftServer.getGlobalEventHandler().addListener(PlayerStartDiggingEvent::class.java) { event ->
        val level = event.instance
        val player = event.player as VanillaSourcePlayer
        val position = event.blockPosition
        val block = event.block

        val handler = block.handler() as? DiggingHandler ?: GLOBAL_DIGGING_HANDLER ?: return@addListener
        val diggingTime = handler.getDiggingTime(level, position, block, player) ?: return@addListener

        player.startDigging(position, diggingTime)
        player.updateDiggingBlockFace(event.blockFace)
    }
    MinecraftServer.getGlobalEventHandler().addListener(PlayerCancelDiggingEvent::class.java) { event ->
        val player = event.player as VanillaSourcePlayer
        player.cancelDigging()
    }
    MinecraftServer.getGlobalEventHandler().addListener(PlayerFinishDiggingEvent::class.java) { event ->
        val player = event.player as VanillaSourcePlayer
        player.cancelDigging()
    }
}