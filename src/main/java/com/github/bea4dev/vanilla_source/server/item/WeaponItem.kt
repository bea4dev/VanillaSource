package com.github.bea4dev.vanilla_source.server.item

import net.kyori.adventure.text.Component
import net.minestom.server.entity.Entity
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

class WeaponItem(
    id: String,
    material: Material,
    displayName: Component?,
    lore: List<Component>?,
    val isMelee: Boolean,
    val damage: Float
) : VanillaSourceItem(id, material, displayName, lore) {

    override fun createBaseItemStack(): ItemStack {
        return ItemStack.of(material)
    }

    override fun onAttack(player: Player, target: Entity, itemStack: ItemStack) {
        // None
    }


}