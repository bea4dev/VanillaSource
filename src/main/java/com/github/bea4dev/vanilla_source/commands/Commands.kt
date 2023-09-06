package com.github.bea4dev.vanilla_source.commands

import net.minestom.server.MinecraftServer

class Commands {

    companion object {
        @JvmStatic
        fun register() {
            val commandManager = MinecraftServer.getCommandManager()
            commandManager.register(StopCommand())
            commandManager.register(ModelCommand())
            commandManager.register(GamemodeCommand())
        }
    }
}