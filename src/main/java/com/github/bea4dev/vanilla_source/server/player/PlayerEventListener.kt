package com.github.bea4dev.vanilla_source.server.player

import com.github.bea4dev.vanilla_source.gui.inventory.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.MinecraftServer
import net.minestom.server.event.inventory.InventoryCloseEvent
import net.minestom.server.event.player.PlayerChatEvent
import net.minestom.server.inventory.InventoryType
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

fun registerPlayerEventListener() {
    playerInventoryListener()
}

fun playerInventoryListener() {
    MinecraftServer.getGlobalEventHandler().addListener(InventoryCloseEvent::class.java) { event ->
        val player = event.player as VanillaSourcePlayer
        val history = player.guiHistory
        synchronized(history) { history.clear() }
    }

    MinecraftServer.getGlobalEventHandler().addListener(PlayerChatEvent::class.java) { event ->
        val G = NONE_BUTTON
        val P = PREVIOUS_PAGE_BUTTON
        val N = NEXT_PAGE_BUTTON
        val A = null

        val frame = Frame(arrayOf(
            G, G, G, G, G, G, G, G, G,
            G, A, A, A, A, A, A, A, G,
            G, A, A, A, A, A, A, A, G,
            G, A, A, A, A, A, A, A, G,
            G, A, A, A, A, A, A, A, G,
            G, G, G, G, G, G, G, P, N,
        ))

        val gui = InventoryGUI(
            Component.translatable("debug.message").decoration(TextDecoration.ITALIC, false),
            InventoryType.CHEST_6_ROW,
            frame,
            NONE_BUTTON
        )
        for (i in 0 until 100) {
            gui.addButton(Button(
                { _, _, _ -> ItemStack.builder(Material.STONE).amount(i).build() },
                { player, _, _, _ -> player.sendMessage(i.toString()) },
                false
            ))
        }

        gui.open(event.player)
    }
}