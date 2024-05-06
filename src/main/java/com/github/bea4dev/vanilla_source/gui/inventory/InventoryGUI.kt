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
import java.util.BitSet
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class InventoryGUI(
    private val id: String,
    private val defaultGUIName: Component,
    private val defaultInventoryType: InventoryType,
    private val defaultFrame: Frame,
    private val fillButton: Button?
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
                val inventoryGUIPage = InventoryGUIPage(
                    this,
                    lock,
                    defaultInventoryType,
                    defaultGUIName,
                    i,
                    defaultFrame,
                    fillButton
                )
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
            var page = 1
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

    fun update() {
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
                    if (last.first.id == this.id) {
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
    private val lock: ReentrantLock,
    private val inventoryType: InventoryType,
    name: Component,
    private val page: Int,
    frame: Frame,
    private val fillButton: Button?
) {
    val inventory = Inventory(inventoryType, name)
    private val buttons = mutableMapOf<Int, Button>()
    private val filledSlot: BitSet = BitSet(inventoryType.size)

    constructor(gui: InventoryGUI, lock: ReentrantLock, inventoryType: InventoryType, name: String, page: Int, frame: Frame, fillButton: Button)
            : this(gui, lock, inventoryType, Component.text(name.replace("%page", page.toString())), page, frame, fillButton)

    init {

        for (i in frame.buttons.indices) {
            val button = frame.buttons[i] ?: continue
            setButton(i, button)
        }

        inventory.addInventoryCondition { player, i, clickType, condition ->
            lock.withLock {
                val button = buttons[i] ?: return@addInventoryCondition
                val info = ButtonClickEventInfo(player, clickType, condition, gui)
                button.clickEvent.accept(info)
                condition.isCancel = true
            }
        }

        if (fillButton != null) {
            for (i in 0 until inventoryType.size) {
                fillSlot(i)
            }
        }
    }

    fun fillSlot(slot: Int) {
        fillButton?.let {
            val itemStack = inventory.getItemStack(slot)
            val button = buttons[slot]

            if (button == null && (itemStack.isAir || itemStack.amount() == 0)) {
                setButton(slot, fillButton)
                filledSlot[slot] = true
            }
        }
    }

    fun setButton(slot: Int, button: Button) {
        buttons[slot] = button
        val info = ButtonItemInfo(gui, page, slot)
        inventory.setItemStack(slot, button.itemProvider.accept(info))
        filledSlot[slot] = false
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

            if (filledSlot[i] || (slotButton == null && (itemStack.isAir || itemStack.amount() == 0))) {
                setButton(i, button)
                result = true
                filledSlot[i] = false
                break
            }
        }

        return result
    }

    fun addItemStack(itemStack: ItemStack): Boolean {
        var result = false

        for (i in 0 until inventoryType.size)  {
            val slotButton = buttons[i]
            val slotItemStack = inventory.getItemStack(i)

            if (filledSlot[i] || (slotButton == null && (slotItemStack.isAir || slotItemStack.amount() == 0))) {
                inventory.setItemStack(i, itemStack)
                result = true
                filledSlot[i] = false
                break
            }
        }

        return result
    }

    fun update() {
        for (entry in buttons.entries) {
            val slot = entry.key
            val button = entry.value
            if (button.updateItem) {
                val info = ButtonItemInfo(gui, page, slot)
                inventory.setItemStack(slot, button.itemProvider.accept(info))
            }
        }
        inventory.update()
    }
}

@FunctionalInterface
fun interface ButtonClickEvent {
    fun accept(info: ButtonClickEventInfo)
}

data class ButtonClickEventInfo(
    val player: Player,
    val clickType: ClickType,
    val condition: InventoryConditionResult,
    val gui: InventoryGUI
)


@FunctionalInterface
fun interface ButtonItemProvider {
    fun accept(info: ButtonItemInfo): ItemStack
}

data class ButtonItemInfo(val gui: InventoryGUI, val page: Int, val slot: Int)

class Button(val itemProvider: ButtonItemProvider, val clickEvent: ButtonClickEvent, val updateItem: Boolean)

class Frame(val buttons: Array<Button?>)

private val NONE_ITEM = ItemStack.builder(Material.GRAY_STAINED_GLASS_PANE)
    .meta { meta -> meta.displayName(Component.text("").color(NamedTextColor.GRAY)) }
    .build()

val NONE_BUTTON = Button({ _ -> NONE_ITEM }, { _ -> }, false)

val NEXT_PAGE_BUTTON = Button(
    { info ->
        val gui = info.gui
        val page = info.page
        val lastPage = gui.lastPage()
        if (page < lastPage) {
            ItemStack.builder(Material.ARROW)
                .displayName(
                    Component.translatable("gui.next_page")
                        .decoration(TextDecoration.UNDERLINED, true)
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
    { info -> (info.player as VanillaSourcePlayer).openGUINextPage() },
    true
)

val PREVIOUS_PAGE_BUTTON = Button(
    { info ->
        val gui = info.gui
        val page = info.page
        if (page > 1) {
            ItemStack.builder(Material.ARROW)
                .displayName(Component.translatable("gui.prev_page")
                    .decoration(TextDecoration.UNDERLINED, true)
                    .decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(" [${page - 1} / ${gui.lastPage()}]")
                        .decoration(TextDecoration.ITALIC, false)
                        .color(NamedTextColor.GRAY))
                )
                .build()
        } else {
            NONE_ITEM
        }
    },
    { info -> (info.player as VanillaSourcePlayer).openGUIPrevPage() },
    true
)

val BACK_BUTTON = Button(
    { _ ->
        ItemStack.builder(Material.OAK_DOOR)
            .displayName(
                Component.translatable("gui.back")
                    .decoration(TextDecoration.UNDERLINED, true)
                    .decoration(TextDecoration.ITALIC, false)
            )
            .build()
    },
    { info -> (info.player as VanillaSourcePlayer).openPrevGUI() },
    false
)

val CLOSE_BUTTON = Button(
    { _ ->
        ItemStack.builder(Material.BARRIER)
            .displayName(
                Component.translatable("gui.close")
                    .color(NamedTextColor.RED)
                    .decoration(TextDecoration.UNDERLINED, true)
                    .decoration(TextDecoration.ITALIC, false)
            )
            .build()
    },
    { info -> info.player.closeInventory() },
    false
)
