package com.github.bea4dev.vanilla_source.server.player

import com.github.bea4dev.vanilla_source.gui.inventory.InventoryGUI
import com.github.bea4dev.vanilla_source.server.entity.isOnGroundStrict
import net.kyori.adventure.translation.Translator
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.effects.Effects
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.network.packet.server.SendablePacket
import net.minestom.server.network.packet.server.play.BlockBreakAnimationPacket
import net.minestom.server.network.packet.server.play.EffectPacket
import net.minestom.server.network.packet.server.play.HitAnimationPacket
import net.minestom.server.network.player.PlayerConnection
import net.minestom.server.potion.Potion
import net.minestom.server.potion.PotionEffect
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import kotlin.math.min


@Suppress("UnstableApiUsage")
open class VanillaSourcePlayer(uuid: UUID, username: String, playerConnection: PlayerConnection) :
    Player(uuid, username, playerConnection) {

    private val tickTasks = ArrayList<Consumer<Long>>()
    val guiHistory = Stack<Pair<InventoryGUI, Int>>()
    private var diggingTick = 0
    private var diggingTime = 0
    private var diggingBlockFace = BlockFace.TOP
    private var diggingBlock: Point? = null
    private var lastGroundPosition = super.position
    private var isPreviousOnGround = this.isOnGroundStrict
    var noFallDamageTick = 0
    var fallDistance = 0.0
    var noFallDamage = false

    init {
        if (locale == null) {
            locale = Translator.parseLocale("ja_JP")
        }
    }

    override fun tick(time: Long) {
        processFallDamage()

        super.tick(time)
        for (tickTask in this.tickTasks) {
            tickTask.accept(time)
        }

        processDiggingTick()

        isPreviousOnGround = this.isOnGroundStrict
        if (isPreviousOnGround) {
            fallDistance = 0.0
            lastGroundPosition = super.position
        }

        if (super.getGameMode() == GameMode.CREATIVE || super.getGameMode() == GameMode.SPECTATOR) {
            fallDistance = 0.0
        }
    }

    private fun processFallDamage() {
        if (noFallDamageTick > 0) {
            fallDistance = 0.0
            noFallDamageTick--
            return
        }

        val tickFallDistance = super.previousPosition.y - super.position.y
        if (tickFallDistance > 0.0) {
            fallDistance += tickFallDistance
        }

        if (noFallDamage
            || !super.onGround
            || isPreviousOnGround
            || super.getGameMode() == GameMode.CREATIVE
            || super.getGameMode() == GameMode.SPECTATOR) {

            return
        }

        val level = super.instance ?: return
        val position = super.position
        val blocks = listOf(
            level.getBlock(position),
            level.getBlock(position.add(0.0, 1.0, 0.0)),
            level.getBlock(position.add(0.0, -1.0, 0.0))
        )
        val inFluid = blocks.stream().anyMatch { block -> block.registry().isLiquid }
        if (inFluid) {
            fallDistance = 0.0
            return
        }


        if (fallDistance > 3.0) {
            val effect = super.getEffect(PotionEffect.JUMP_BOOST)
            val reduce = if (effect != null) {
                effect.potion().amplifier() + 1
            } else {
                0
            }
            val damage = fallDistance.toFloat() - 3.0F - reduce.toFloat()
            if (damage >= 1.0) {
                super.damage(DamageType.FALL, damage)
                super.sendPacketsToViewers(HitAnimationPacket(super.getEntityId(), super.position.yaw))
            }
            fallDistance = 0.0
        }
    }

    fun addTickTask(task: Consumer<Long>) {
        this.tickTasks.add(task)
    }

    fun openGUINextPage() {
        synchronized(guiHistory) {
            if (guiHistory.isEmpty()) { return@synchronized }

            val last = guiHistory.peek()
            val gui = last.first
            val page = last.second
            if (page < gui.lastPage()) {
                gui.openPage(this, page + 1)
            }
        }
    }

    fun openGUIPrevPage() {
        synchronized(guiHistory) {
            if (guiHistory.isEmpty()) { return@synchronized }

            val last = guiHistory.peek()
            val gui = last.first
            val page = last.second
            if (page > 1) {
                gui.openPage(this, page - 1)
            }
        }
    }

    fun openPrevGUI() {
        synchronized(guiHistory) {
            if (guiHistory.isEmpty()) { return@synchronized }

            guiHistory.pop()
            if (guiHistory.isEmpty()) {
                super.closeInventory()
                return@synchronized
            }

            val last = guiHistory.peek()
            last.first.openPage(this, last.second)
        }
    }

    override fun sendPacket(packet: SendablePacket) {
        super.sendPacket(rewritePacket(packet))
    }

    override fun sendPackets(vararg packets: SendablePacket) {
        for (packet in packets) {
            sendPacket(packet)
        }
    }

    override fun sendPackets(packets: MutableCollection<SendablePacket>) {
        for (packet in packets) {
            sendPacket(packet)
        }
    }

    private fun rewritePacket(packet: SendablePacket): SendablePacket {
        return packet
    }

    @Synchronized
    fun processDiggingTick() {
        val position = diggingBlock ?: return
        if (diggingTick == diggingTime) {
            val level = super.instance ?: return
            val block = level.getBlock(position)

            level.breakBlock(this, position, diggingBlockFace)
            super.sendPacket(EffectPacket(Effects.BLOCK_BREAK.id, position, block.stateId().toInt(), false))

            cancelDigging()
            return
        }

        val stage = min((diggingTick.toDouble() / diggingTime.toDouble() * 10.0).toInt(), 9).toByte()
        super.sendPacketToViewersAndSelf(BlockBreakAnimationPacket(-super.getEntityId(), position, stage))

        if (diggingTick < diggingTime) {
            diggingTick++
        }
    }

    @Synchronized
    fun startDigging(blockPosition: Point, time: Int) {
        diggingTick = 0
        diggingTime = time
        diggingBlock = blockPosition
        super.addEffect(Potion(PotionEffect.MINING_FATIGUE, -1, -1))
    }

    @Synchronized
    fun cancelDigging() {
        val position = diggingBlock
        if (position != null) {
            super.sendPacketToViewersAndSelf(BlockBreakAnimationPacket(-super.getEntityId(), position, -1))
        }
        diggingBlock = null
        super.removeEffect(PotionEffect.MINING_FATIGUE)
    }

    @Synchronized
    fun updateDiggingBlockFace(blockFace: BlockFace) {
        diggingBlockFace = blockFace
    }

    override fun setGameMode(gameMode: GameMode): Boolean {
        fallDistance = 0.0
        return super.setGameMode(gameMode)
    }

    override fun teleport(position: Pos, chunks: LongArray?, flags: Int): CompletableFuture<Void> {
        val completableFuture = CompletableFuture<Void>()
        noFallDamageTick = Int.MAX_VALUE
        super.teleport(position, chunks, flags).thenAccept {
            fallDistance = 0.0
            noFallDamageTick = 2
            completableFuture.complete(null)
        }
        return completableFuture
    }

}