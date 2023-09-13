package com.github.bea4dev.vanilla_source.server.item

import net.kyori.adventure.text.Component
import net.minestom.server.entity.Entity
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.tag.Tag

abstract class VanillaSourceItem(
    val id: String,
    val material: Material,
    val displayName: Component?,
    val lore: List<Component>?
) {

    companion object {
        @JvmStatic
        val idTag = Tag.String("vanilla_source_item_id")
    }

    fun createItemStack(): ItemStack {
        val base = this.createBaseItemStack()
        return base.withMeta { builder ->
            val displayName = displayName
            if (displayName != null) {
                builder.displayName(displayName)
            }

            val lore = lore
            if (lore != null) {
                builder.lore(lore)
            }

            builder.setTag(idTag, this.id)
        }
    }

    protected abstract fun createBaseItemStack(): ItemStack

    abstract fun onAttack(player: Player, target: Entity?, itemStack: ItemStack)

}