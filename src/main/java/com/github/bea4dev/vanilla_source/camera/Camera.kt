package com.github.bea4dev.vanilla_source.camera

import com.github.bea4dev.vanilla_source.server.player.VanillaSourcePlayer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.GameMode
import net.minestom.server.network.packet.server.play.CameraPacket
import net.minestom.server.network.packet.server.play.DestroyEntitiesPacket
import net.minestom.server.network.packet.server.play.EntityTeleportPacket
import java.util.concurrent.CompletableFuture

class Camera(position: Pos): Entity(EntityType.BOAT) {
    private val players = mutableListOf<VanillaSourcePlayer>()

    init {
        super.setInvisible(true)
        super.setNoGravity(true)
        teleport(position)
    }

    @Synchronized
    fun addPLayer(player: VanillaSourcePlayer) {
        player.gameMode = GameMode.SPECTATOR
        val spawnPacket = EntityType.BOAT.registry().spawnType.getSpawnPacket(this)
        val cameraPacket = CameraPacket(this)
        player.sendPackets(spawnPacket, cameraPacket)
        players.add(player)
    }

    @Synchronized
    override fun remove() {
        val destroyPacket = DestroyEntitiesPacket(super.getEntityId())
        for (player in players) {
            player.sendPacket(destroyPacket)
        }
    }

    @Suppress("UnstableApiUsage")
    @Synchronized
    override fun teleport(position: Pos): CompletableFuture<Void> {
        super.lastSyncedPosition = super.position
        super.previousPosition = super.position
        super.position = position
        this.setView(position.yaw(), position.pitch())

        val teleportPacket = EntityTeleportPacket(super.getEntityId(), position, false)

        for (player in players) {
            synchronized(player) {
                if (player.instance != null) {
                    player.teleport(position)
                }
            }
            player.sendPacket(teleportPacket)
        }

        return CompletableFuture.completedFuture(null)
    }
}