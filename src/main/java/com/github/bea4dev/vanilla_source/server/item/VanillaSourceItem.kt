package com.github.bea4dev.vanilla_source.server.item

import net.kyori.adventure.text.Component
import net.minestom.server.coordinate.Point
import net.minestom.server.entity.Entity
import net.minestom.server.entity.Player
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.item.ItemComponent
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.tag.Tag


fun ItemStack.getItem(): VanillaSourceItem? {
    return VanillaSourceItem.fromItemStack(this)
}

fun ItemStack.getItemId(): String? {
    return VanillaSourceItem.getItemId(this)
}


abstract class VanillaSourceItem(
    val id: String,
    val material: Material,
    val customModelData: Int,
    val displayName: Component?,
    val lore: List<Component>?
) {

    companion object {
        @JvmStatic
        val idTag = Tag.String("vanilla_source_item_id")

        @JvmStatic
        fun fromItemStack(itemStack: ItemStack): VanillaSourceItem? {
            val id = getItemId(itemStack) ?: return null
            return ItemRegistry.INSTANCE[id]
        }

        @JvmStatic
        fun getItemId(itemStack: ItemStack): String? {
            return itemStack.getTag(idTag)
        }
    }

    fun createItemStack(): ItemStack {
        val base = this.createBaseItemStack()
        return base.with { builder ->
            val displayName = displayName
            if (displayName != null) {
                builder.set(ItemComponent.ITEM_NAME, displayName)
            }

            val lore = lore
            if (lore != null) {
                builder.set(ItemComponent.LORE, lore)
            }

            builder.set(ItemComponent.CUSTOM_MODEL_DATA, customModelData)

            builder.setTag(idTag, this.id)
        }
    }

    protected open fun createBaseItemStack(): ItemStack {
        return ItemStack.of(material)
    }

    abstract fun onEntityAttack(player: Player, target: Entity, itemStack: ItemStack)

    abstract fun onAnimation(player: Player, itemStack: ItemStack)

    abstract fun onRightClick(player: Player, itemStack: ItemStack)

    abstract fun onRightClickEntity(player: Player, entity: Entity, point: Point, itemStack: ItemStack)

    abstract fun onRightClickBlock(player: Player, block: Block, blockPosition: Point, cursorPosition: Point, blockFace: BlockFace, itemStack: ItemStack)

}