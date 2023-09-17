package com.github.bea4dev.vanilla_source.natives

import com.github.bea4dev.vanilla_source.server.VanillaSource
import net.minestom.server.MinecraftServer
import net.minestom.server.event.instance.InstanceChunkLoadEvent
import net.minestom.server.event.instance.InstanceRegisterEvent


fun registerNativeChunkListener() {
    if (!VanillaSource.getServer().nativeManager.isEnabled()) {
        return
    }

    // On instance create
    MinecraftServer.getGlobalEventHandler().addListener(InstanceRegisterEvent::class.java) { event ->
        val level = event.instance
        NativeBridge.registerWorld(level.getNativeID(), level.dimensionType.height, level.dimensionType.minY)
    }

    // On chunk load
    MinecraftServer.getGlobalEventHandler().addListener(InstanceChunkLoadEvent::class.java) { event ->
        val sender = NativeChunkSender.getChunkSender(event.instance)
        sender.registerChunk(event.instance, event.chunk)
    }
}