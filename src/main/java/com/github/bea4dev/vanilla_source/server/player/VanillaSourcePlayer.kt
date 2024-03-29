package com.github.bea4dev.vanilla_source.server.player

import com.github.bea4dev.vanilla_source.gui.inventory.InventoryGUI
import net.kyori.adventure.translation.GlobalTranslator
import net.kyori.adventure.translation.Translator
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.network.packet.server.SendablePacket
import net.minestom.server.network.packet.server.play.OpenWindowPacket
import net.minestom.server.network.packet.server.play.WindowItemsPacket
import net.minestom.server.network.player.PlayerConnection
import java.util.*
import java.util.function.Consumer
import kotlin.collections.ArrayList

@Suppress("UnstableApiUsage")
open class VanillaSourcePlayer(uuid: UUID, username: String, playerConnection: PlayerConnection) :
    Player(uuid, username, playerConnection) {

    private val tickTasks = ArrayList<Consumer<Long>>()
    val guiHistory = Stack<Pair<InventoryGUI, Int>>()

    init {
        if (locale == null) {
            locale = Translator.parseLocale("ja_JP")
        }
    }

    override fun tick(time: Long) {
        super.tick(time)
        for (tickTask in this.tickTasks) {
            tickTask.accept(time)
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
        if (packet is WindowItemsPacket) {
            val items = mutableMapOf<Int, ItemStack>()
            for (i in packet.items.indices) {
                val item = packet.items[i]
                val displayName = item.displayName ?: continue
                val translated = GlobalTranslator.render(displayName, locale!!)

                if (displayName != translated) {
                    items[i] = item.withDisplayName(translated)
                }
            }

            if (items.isNotEmpty()) {
                val itemList = mutableListOf<ItemStack>()
                for (i in packet.items.indices) {
                    val original = packet.items[i]
                    val new = items[i]
                    if (new == null) {
                        itemList.add(original)
                    } else {
                        itemList.add(new)
                    }
                }

                return WindowItemsPacket(packet.windowId, packet.stateId, itemList, packet.carriedItem)
            }
        }

        if (packet is OpenWindowPacket) {
            val title = packet.title
            val translated = GlobalTranslator.render(title, locale!!)

            if (title != translated) {
                return OpenWindowPacket(packet.windowId, packet.windowType, translated)
            }
        }

        return packet
    }

}