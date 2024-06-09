package com.github.bea4dev.vanilla_source.server.level

import net.minestom.server.MinecraftServer
import net.minestom.server.utils.NamespaceID
import net.minestom.server.world.DimensionType


private val NETHER_EFFECT_OVERWORLD = DimensionType.builder(NamespaceID.from("vanilla_source:nether_effect_overworld"))
    .ultrawarm(false)
    .natural(true)
    .piglinSafe(false)
    .respawnAnchorWorks(false)
    .bedWorks(true)
    .hasRaids(true)
    .hasSkylight(true)
    .hasCeiling(false)
    .fixedTime(null)
    .logicalHeight(384)
    .effects("the_nether")
    .infiniburn("minecraft:infiniburn_overworld")
    .build()

fun registerDimensions() {
    MinecraftServer.getDimensionTypeRegistry().register(NETHER_EFFECT_OVERWORLD)
}