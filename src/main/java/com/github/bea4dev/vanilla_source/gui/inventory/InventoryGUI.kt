package com.github.bea4dev.vanilla_source.gui.inventory

import com.github.bea4dev.vanilla_source.server.player.VanillaSourcePlayer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.entity.Player
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.inventory.click.ClickType
import net.minestom.server.inventory.condition.InventoryConditionResult
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class InventoryGUI(
    private val defaultGUIName: Component,
    private val defaultInventoryType: InventoryType,
    private val defaultFrame: Frame
) {
    private val pages = mutableListOf<InventoryGUIPage>()
    private val lock = ReentrantLock()

    init {
        createPage(1)
    }

    fun createPage(page: Int) {
        if (page < 1) {
            throw IllegalArgumentException("Invalid page number : $page")
        }

        if (page <= pages.size) {
            return
        }

        lock.withLock {
            for (i in (pages.size + 1)..page) {
                val inventoryGUIPage = InventoryGUIPage(this, defaultInventoryType, defaultGUIName, i, defaultFrame)
                pages.add(inventoryGUIPage)
            }
            update()
        }
    }

    fun addPage() {
        lock.withLock {
            createPage(pages.size + 1)
            update()
        }
    }

    fun setCustomPage(page: Int, inventoryGUIPage: InventoryGUIPage) {
        lock.withLock {
            pages[page - 1] = inventoryGUIPage
            update()
        }
    }

    fun addCustomPage(inventoryGUIPage: InventoryGUIPage) {
        lock.withLock {
            pages.add(inventoryGUIPage)
            update()
        }
    }

    fun setButton(page: Int, slot: Int, button: Button) {
        lock.withLock {
            createPage(page)
            pages[page - 1].setButton(slot, button)
            update()
        }
    }

    fun setItemStack(page: Int, slot: Int, itemStack: ItemStack) {
        lock.withLock {
            createPage(page)
            pages[page - 1].setItemStack(slot, itemStack)
            update()
        }
    }

    fun addButton(button: Button) {
        lock.withLock {
            var page = 1
            while (true) {
                if (page > pages.size) {
                    addPage()
                }

                val currentPage = pages[page - 1]
                val result = currentPage.addButton(button)
                if (result) {
                    break
                }
                page++
            }
            update()
        }
    }

    fun addItemStack(itemStack: ItemStack) {
        lock.withLock {
            var page = 0
            while (true) {
                if (page > pages.size) {
                    addPage()
                }

                val currentPage = pages[page - 1]
                val result = currentPage.addItemStack(itemStack)
                if (result) {
                    break
                }
                page++
            }
            update()
        }
    }

    fun fillButton(button: Button) {
        lock.withLock {
            for (page in pages) {
                page.fillButton(button)
            }
        }
    }

    fun fillItemStack(itemStack: ItemStack) {
        lock.withLock {
            for (page in pages) {
                page.fillItemStack(itemStack)
            }
        }
    }

    private fun update() {
        lock.withLock {
            for (page in pages) {
                page.update()
            }
        }
    }

    fun open(player: Player) {
        openPage(player, 1)
    }

    fun openPage(player: Player, page: Int) {
        lock.withLock {
            val inventoryGUIPage = pages[page - 1]
            player.openInventory(inventoryGUIPage.inventory)

            val history = (player as VanillaSourcePlayer).guiHistory
            synchronized(history) {
                if (history.isEmpty()) {
                    history.push(Pair(this, page))
                } else {
                    val last = history.peek()
                    if (last.first == this) {
                        history.pop()
                    }
                    history.push(Pair(this, page))
                }
            }
        }
    }

    fun lastPage(): Int {
        return lock.withLock { pages.size }
    }

}

class InventoryGUIPage(
    private val gui: InventoryGUI,
    private val inventoryType: InventoryType,
    name: Component,
    private val page: Int,
    frame: Frame
) {
    val inventory = Inventory(inventoryType, name)
    private val buttons = mutableMapOf<Int, Button>()

    constructor(gui: InventoryGUI, inventoryType: InventoryType, name: String, page: Int, frame: Frame)
            : this(gui, inventoryType, Component.text(name.replace("%page", page.toString())), page, frame)

    init {
        for (i in frame.buttons.indices) {
            val button = frame.buttons[i] ?: continue
            setButton(i, button)
        }

        inventory.addInventoryCondition { player, i, clickType, condition ->
            val button = buttons[i] ?: return@addInventoryCondition
            button.clickEvent.accept(player, clickType, condition, gui)
            condition.isCancel = true
        }
    }

    fun setButton(slot: Int, button: Button) {
        buttons[slot] = button
        inventory.setItemStack(slot, button.itemProvider.accept(gui, page, slot))
    }

    fun setItemStack(slot: Int, itemStack: ItemStack) {
        inventory.setItemStack(slot, itemStack)
        buttons.remove(slot)
    }

    fun addButton(button: Button): Boolean {
        var result = false

        for (i in 0 until inventoryType.size)  {
            val slotButton = buttons[i]
            val itemStack = inventory.getItemStack(i)

            if (slotButton == null && itemStack.isAir) {
                setButton(i, button)
                result = true
                break
            }
        }

        return result
    }

    fun addItemStack(itemStack: ItemStack): Boolean {
        return inventory.addItemStack(itemStack)
    }

    fun fillButton(button: Button) {
        for (i in 0 until inventoryType.size) {
            if (inventory.getItemStack(i).isAir) {
                setButton(i, button)
            }
        }
    }

    fun fillItemStack(itemStack: ItemStack) {
        for (i in 0 until inventoryType.size) {
            if (inventory.getItemStack(i).isAir) {
                setItemStack(i, itemStack)
            }
        }
    }

    fun update() {
        for (entry in buttons.entries) {
            val slot = entry.key
            val button = entry.value
            if (button.updateItem) {
                inventory.setItemStack(slot, button.itemProvider.accept(gui, page, slot))
            }
        }
        inventory.update()
    }
}

@FunctionalInterface
fun interface ButtonClickEvent {
    fun accept(player: Player, clickType: ClickType, condition: InventoryConditionResult, gui: InventoryGUI)
}

@FunctionalInterface
fun interface ButtonItemProvider {
    fun accept(gui: InventoryGUI, page: Int, slot: Int): ItemStack
}

class Button(val itemProvider: ButtonItemProvider, val clickEvent: ButtonClickEvent, val updateItem: Boolean)

class Frame(val buttons: Array<Button?>)

private val NONE_ITEM = ItemStack.builder(Material.GRAY_STAINED_GLASS_PANE)
    .meta { meta -> meta.displayName(Component.text("").color(NamedTextColor.GRAY)) }
    .build()

val NONE_BUTTON = Button(
    { _, _, _ -> NONE_ITEM },
    { _, _, _, _ -> },
    false
)

val NEXT_PAGE_BUTTON = Button(
    { gui, page, _ ->
        val lastPage = gui.lastPage()
        if (page < lastPage) {
            ItemStack.builder(Material.ARROW)
                .displayName(
                    Component.translatable("gui.next_page")
                        .decoration(TextDecoration.ITALIC, false)
                        .append(Component.text(" [${page + 1} / ${lastPage}]")
                            .decoration(TextDecoration.ITALIC, false)
                            .color(NamedTextColor.GRAY))
                )
                .build()
        } else {
            NONE_ITEM
        }
    },
    { player, _, _, _ -> (player as VanillaSourcePlayer).openGUINextPage() },
    true
)

val PREVIOUS_PAGE_BUTTON = Button(
    { gui, page, _ ->
        if (page > 1) {
            ItemStack.builder(Material.ARROW)
                .meta { meta ->
                    meta.displayName(Component.translatable("gui.prev_page")
                        .decoration(TextDecoration.ITALIC, false)
                        .append(Component.text(" [${page - 1} / ${gui.lastPage()}]")
                            .decoration(TextDecoration.ITALIC, false)
                            .color(NamedTextColor.GRAY))
                    )
                }
                .build()
        } else {
            NONE_ITEM
        }
    },
    { player, _, _, _ -> (player as VanillaSourcePlayer).openGUIPrevPage() },
    true
)

