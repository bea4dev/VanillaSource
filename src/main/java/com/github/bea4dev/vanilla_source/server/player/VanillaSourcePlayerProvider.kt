package com.github.bea4dev.vanilla_source.server.player

import net.minestom.server.entity.Player
import net.minestom.server.network.PlayerProvider
import net.minestom.server.network.player.PlayerConnection
import java.util.*

class VanillaSourcePlayerProvider: PlayerProvider {
    override fun createPlayer(uuid: UUID, name: String, connection: PlayerConnection): Player {
        return VanillaSourcePlayer(uuid, name, connection)
    }

}