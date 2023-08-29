package com.github.bea4dev.vanilla_source.commands

import com.github.bea4dev.vanilla_source.server.VanillaSource
import net.minestom.server.command.builder.Command

class StopCommand : Command("stop") {

    init {
        addSyntax({ _, _ ->
            VanillaSource.getServer().stop()
        })
    }
}