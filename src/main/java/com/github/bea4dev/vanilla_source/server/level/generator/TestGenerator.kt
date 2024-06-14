package com.github.bea4dev.vanilla_source.server.level.generator

import de.articdive.jnoise.generators.noisegen.opensimplex.FastSimplexNoiseGenerator
import de.articdive.jnoise.pipeline.JNoise
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.generator.GenerationUnit
import net.minestom.server.instance.generator.Generator
import net.minestom.server.registry.DynamicRegistry
import net.minestom.server.sound.SoundEvent
import net.minestom.server.utils.NamespaceID
import net.minestom.server.world.biome.Biome
import net.minestom.server.world.biome.BiomeEffects
import kotlin.math.floor

@Suppress("UnstableApiUsage")
class TestGenerator: Generator {

    // Noise used for the height
    private val noise: JNoise = JNoise.newBuilder()
        .fastSimplex(FastSimplexNoiseGenerator.newBuilder().setSeed(0).build())
        .scale(0.005)
        .build()

    private val layers: List<Block>
    private val biomeKey: DynamicRegistry.Key<Biome>

    init {
        val layers = mutableListOf<Block>()
        for (i in 1..8) {
            layers.add(Block.SNOW.withProperty("layers", i.toString()))
        }
        this.layers = layers

        val color = NamedTextColor.WHITE.value()
        val biome = Biome.builder()
            .effects(BiomeEffects(
                color,
                color,
                color,
                color,
                color,
                color,
                null,
                null,
                SoundEvent.MUSIC_NETHER_NETHER_WASTES.namespace(),
                null,
                null,
                BiomeEffects.Music(SoundEvent.MUSIC_NETHER_NETHER_WASTES.namespace(), 0, 1000, true)
            ))
            .precipitation(Biome.Precipitation.SNOW)
            .temperature(0.0F)
            .build()
        biomeKey = MinecraftServer.getBiomeRegistry().register("custom:test", biome)
    }

    override fun generate(unit: GenerationUnit) {
        val start = unit.absoluteStart()
        for (x in 0..<unit.size().x().toInt()) {
            for (z in 0..<unit.size().z().toInt()) {
                val bottom = start.add(x.toDouble(), 0.0, z.toDouble())

                val height = synchronized(noise) {
                    // Synchronization is necessary for JNoise
                    noise.evaluateNoise(bottom.x(), bottom.z()) * 16
                }

                // * 16 means the height will be between -16 and +16
                unit.modifier()
                    .fill(bottom, bottom.add(1.0, 0.0, 1.0).withY(height), Block.STONE)

                val layerIndex = ((height - floor(height)) * 8).toInt()
                val layer = layers[layerIndex]
                unit.modifier().setBlock(bottom.withY(height), layer)
            }
        }

        unit.modifier().fillBiome(biomeKey)
    }
}