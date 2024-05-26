package com.github.bea4dev.vanilla_source.server.listener

import net.minestom.server.MinecraftServer
import net.minestom.server.entity.LivingEntity
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.event.entity.EntityTickEvent

fun registerEntityListener() {
    registerEntityVoidDamageListener()
}

private fun registerEntityVoidDamageListener() {
    MinecraftServer.getGlobalEventHandler().addListener(EntityTickEvent::class.java) { event ->
        val entity = event.entity
        if (entity.isRemoved) {
            return@addListener
        }

        val level = entity.instance ?: return@addListener
        if (entity.position.y < level.dimensionType.minY) {
            if (entity is LivingEntity && !entity.isDead) {
                entity.damage(DamageType.FALL, 20.0F)
            }
            if (entity !is LivingEntity) {
                entity.scheduler().scheduleEndOfTick { entity.remove() }
            }
        }
    }
}