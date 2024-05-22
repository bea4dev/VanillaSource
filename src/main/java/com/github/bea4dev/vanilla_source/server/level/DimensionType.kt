package com.github.bea4dev.vanilla_source.server.level

import net.minestom.server.MinecraftServer
import net.minestom.server.utils.NamespaceID
import net.minestom.server.world.DimensionType


private val NETHER_EFFECT_OVERWORLD = DimensionType.builder(NamespaceID.from("vanilla_source:nether_effect_overworld"))
    .ultrawarm(false)
    .natural(true)
    .piglinSafe(false)
    .respawnAnchorSafe(false)
    .bedSafe(true)
    .raidCapable(true)
    .skylightEnabled(false)
    .ceilingEnabled(false)
    .fixedTime(null)
    .logicalHeight(384)
    .ambientLight(0.5F)
    .effects("the_nether")
    .infiniburn(NamespaceID.from("minecraft:infiniburn_overworld"))
    .build()

fun registerDimensions() {
    MinecraftServer.getDimensionTypeManager().addDimension(NETHER_EFFECT_OVERWORLD)
}