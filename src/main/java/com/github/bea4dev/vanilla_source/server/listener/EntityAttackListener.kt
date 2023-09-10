package com.github.bea4dev.vanilla_source.server.listener

import com.github.bea4dev.vanilla_source.server.entity.EnemyModelEntity
import com.github.bea4dev.vanilla_source.server.item.ItemRegistry
import com.github.bea4dev.vanilla_source.server.item.VanillaSourceItem
import com.github.bea4dev.vanilla_source.server.item.WeaponItem
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.event.player.PlayerHandAnimationEvent
import org.slf4j.LoggerFactory
import team.unnamed.hephaestus.view.BaseBoneView

private val logger = LoggerFactory.getLogger("EntityAttackListener")
private const val DEFAULT_REACH = 2.5

fun registerEntityAttackListener() {
    MinecraftServer.getGlobalEventHandler().addListener(PlayerHandAnimationEvent::class.java) { event ->
        handlePlayerAttack(event.player)
    }
}


fun handlePlayerAttack(player: Player) {
    val itemStack = player.itemInMainHand
    val id = itemStack.meta().getTag(VanillaSourceItem.idTag) ?: return

    val item = ItemRegistry.INSTANCE[id]
    if (item == null) {
        logger.warn("Unknown item id '${id}'!")
        return
    }

    val reach = if (item is WeaponItem) { item.reach } else { DEFAULT_REACH }
    val target = player.getLineOfSightEntity(reach) { entity ->
        // Ignore bone entity
        val meta = entity.entityMeta
        entity !is BaseBoneView && !(entity.entityType == EntityType.ARMOR_STAND && meta.isInvisible && meta.isSilent)
    } ?: return

    if (target is EnemyModelEntity) {
        target.onAttacked(player, item)
    }
    item.onAttack(player, target, itemStack)
}