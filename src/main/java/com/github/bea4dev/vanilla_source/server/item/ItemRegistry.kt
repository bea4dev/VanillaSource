package com.github.bea4dev.vanilla_source.server.item

import com.github.bea4dev.vanilla_source.resource.FreezableResource
import net.kyori.adventure.text.Component
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

class ItemRegistry : FreezableResource() {

    companion object {
        @JvmStatic
        val INSTANCE = ItemRegistry()

        fun freezeRegistry() {
            INSTANCE.freeze()
        }

        @JvmStatic
        fun fromItemStack(itemStack: ItemStack): VanillaSourceItem? {
            val id = itemStack.getTag(VanillaSourceItem.idTag) ?: return null
            return INSTANCE[id]
        }
    }

    private val items = mutableMapOf<String, VanillaSourceItem>()

    operator fun get(id: String): VanillaSourceItem? {
        return items[id]
    }

    operator fun set(id: String, item: VanillaSourceItem) {
        assert()
        items[id] = item
    }

}