package com.github.bea4dev.vanilla_source.server.item

import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Entity
import net.minestom.server.entity.Player
import net.minestom.server.entity.Player.Hand
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.particle.Particle
import net.minestom.server.particle.ParticleCreator
import net.minestom.server.sound.SoundEvent

class WeaponItem(
    id: String,
    material: Material,
    customModelData: Int,
    displayName: Component?,
    lore: List<Component>?,
    val isMelee: Boolean,
    val damage: Float,
    val reach: Double,
    private val attackSound: Sound = Sound.sound(SoundEvent.ENTITY_PLAYER_ATTACK_SWEEP, Sound.Source.PLAYER, 0.4F, 0.4F),
    private val attackParticle: Particle = Particle.SWEEP_ATTACK,
    val guardSound: Sound = Sound.sound(SoundEvent.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, Sound.Source.PLAYER, 0.7F, 1.5F)
) : VanillaSourceItem(id, material, customModelData, displayName, lore) {
    override fun createBaseItemStack(): ItemStack {
        return ItemStack.of(material)
    }

    @Suppress("UnstableApiUsage")
    override fun onEntityAttack(player: Player, target: Entity, itemStack: ItemStack) {
        player.playSound(this.attackSound)
        val position = player.position.add(0.0, player.eyeHeight, 0.0).add(player.position.direction().mul(1.5))
        val packet = ParticleCreator.createParticlePacket(
            this.attackParticle,
            position.x,
            position.y,
            position.z,
            0.0F,
            0.0F,
            0.0F,
            0
        )
        player.sendPacketToViewersAndSelf(packet)
    }

    override fun onAnimation(player: Player, itemStack: ItemStack) {
        // None
    }


}