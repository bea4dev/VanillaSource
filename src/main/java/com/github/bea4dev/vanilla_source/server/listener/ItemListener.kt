package com.github.bea4dev.vanilla_source.server.listener

import com.github.bea4dev.vanilla_source.server.entity.EnemyModelEntity
import com.github.bea4dev.vanilla_source.server.item.getItem
import com.github.bea4dev.vanilla_source.server.item.getItemId
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.event.entity.EntityAttackEvent
import net.minestom.server.event.player.PlayerHandAnimationEvent
import org.slf4j.LoggerFactory
import team.unnamed.hephaestus.view.BaseBoneView

private val logger = LoggerFactory.getLogger("EntityAttackListener")
//private const val DEFAULT_REACH = 2.5

fun registerItemListener() {
    MinecraftServer.getGlobalEventHandler().addListener(EntityAttackEvent::class.java) { event ->
        val entity = event.entity
        if (entity !is Player) { return@addListener }

        handlePlayerAttack(entity, event.target)
    }
    MinecraftServer.getGlobalEventHandler().addListener(PlayerHandAnimationEvent::class.java) { event ->
        handlePlayerHandAnimation(event.player)

        if (event.player.isSneaking) {
            val entity = Entity(EntityType.ARMOR_STAND)
            entity.setInstance(event.player.instance, event.player.position)
            entity.spawn()
        }
    }
}




private fun handlePlayerAttack(player: Player, target: Entity) {
    val itemStack = player.itemInMainHand
    val item = itemStack.getItem()

    if (item == null) {
        val id = itemStack.getItemId()
        if (id != null) {
            logger.warn("Unknown item id '${itemStack.getItemId()}'!")
        }
        return
    }

    //val reach = if (item is WeaponItem) { item.reach } else { DEFAULT_REACH }
    // Ignore bone entity
    val meta = target.entityMeta
    if (target is BaseBoneView || (target.entityType == EntityType.ARMOR_STAND && meta.isInvisible && meta.isSilent)) {
        return
    }

    if (target is EnemyModelEntity) {
        target.onAttacked(player, item)
    }
    item.onEntityAttack(player, target, itemStack)
}


private fun handlePlayerHandAnimation(player: Player) {

}