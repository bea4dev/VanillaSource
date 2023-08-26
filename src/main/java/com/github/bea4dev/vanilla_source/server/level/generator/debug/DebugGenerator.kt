package com.github.bea4dev.vanilla_source.server.level.generator.debug

import com.github.bea4dev.vanilla_source.server.level.generator.ChunkGenerator
import net.minestom.server.instance.Chunk
import net.minestom.server.instance.DynamicChunk
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.generator.GenerationUnit
import net.minestom.server.instance.light.LightCompute

class DebugGenerator: ChunkGenerator {

    override fun generate(unit: GenerationUnit) {
        unit.modifier().setAll { x, y, z ->
            val chunkX = x shr 4
            val chunkZ = z shr 4

            return@setAll if (y <= 64) {
                if ((chunkX + chunkZ) % 2 == 0) {
                    Block.LIGHT_BLUE_CONCRETE
                } else {
                    Block.WHITE_CONCRETE
                }
            } else {
                Block.AIR
            }
        }
    }

    @Suppress("UnstableApiUsage")
    override fun createChunk(instance: Instance, chunkX: Int, chunkZ: Int): Chunk {
        val chunk = DynamicChunk(instance, chunkX, chunkZ)
        for (sectionY in chunk.sections.indices) {
            val section = chunk.sections[sectionY]
            var lightArray = section.skyLight().array()
            if (lightArray == null || lightArray.isEmpty()) {
                lightArray = LightCompute.emptyContent.clone()
            }

            for (x in 0..<16) {
                for (y in 0..<16) {
                    for (z in 0..<16) {
                        setLight(lightArray, x, y, z, 15)
                    }
                }
            }
            section.skyLight().set(lightArray)
        }
        return chunk
    }


    private fun setLight(light: ByteArray, x: Int, y: Int, z: Int, value: Int) {
        val index = (y shl 8) or (z shl 4) or x
        val shift = index and 1 shl 2
        val i = index ushr 1
        light[i] = (light[i].toInt() and (0xF0 ushr shift) or (value shl shift)).toByte()
    }

}