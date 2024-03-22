package com.github.bea4dev.vanilla_source.gui.inventory

import net.minestom.server.entity.Player
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.inventory.click.ClickType
import net.minestom.server.inventory.condition.InventoryConditionResult
import net.minestom.server.item.ItemStack
import java.util.concurrent.CompletableFuture
import java.util.concurrent.locks.ReentrantLock

class InventoryGUI {
    private val pages = mutableListOf<InventoryGUIPage>()
    private val lock = ReentrantLock()

    fun getInventory(): CompletableFuture<Inventory> {
        val future = CompletableFuture<Inventory>()

        val inventory = Inventory(InventoryType.CHEST_6_ROW, "GUI")
        inventory.update()

        inventory.addInventoryCondition { player, i, clickType, inventoryConditionResult ->  }

        return future
    }

}

class InventoryGUIPage {
    private val buttons = mutableListOf<Button?>()
}

@FunctionalInterface
interface ButtonClickEvent {
    fun accept(player: Player, clickType: ClickType, condition: InventoryConditionResult)
}

@FunctionalInterface
interface ButtonItemProvider {
    fun accept(page: Int): ItemStack
}

class Button(val itemProvider: ButtonItemProvider, val clickEvent: ButtonClickEvent)

class Frame(val buttons: MutableList<Button?>)