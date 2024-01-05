package com.github.bea4dev.vanilla_source.server.player

import net.minestom.server.entity.Player
import net.minestom.server.network.player.PlayerConnection
import java.util.*
import java.util.function.Consumer
import kotlin.collections.ArrayList

class VanillaSourcePlayer(uuid: UUID, username: String, playerConnection: PlayerConnection) :
    Player(uuid, username, playerConnection) {

    private val tickTasks = ArrayList<Consumer<Long>>()

    override fun tick(time: Long) {
        super.tick(time)
        for (tickTask in this.tickTasks) {
            tickTask.accept(time)
        }
    }

}