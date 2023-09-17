package com.github.bea4dev.vanilla_source.natives

import net.minestom.server.instance.Chunk
import net.minestom.server.instance.Instance
import java.util.WeakHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal const val SECTION_BLOCKS_LENGTH = 16 * 16 * 16

internal class NativeChunkSender private constructor(level: Instance) {

    companion object {
        private val senderMap = WeakHashMap<Instance, NativeChunkSender>()
        private val lock = ReentrantLock()

        @JvmStatic
        fun getChunkSender(level: Instance): NativeChunkSender {
            return lock.withLock {
                senderMap.computeIfAbsent(level) { NativeChunkSender(level) }
            }
        }
    }

    private val palette: IntArray

    init {
        val minSection = level.dimensionType.minY / Chunk.CHUNK_SECTION_SIZE
        val maxSection = (level.dimensionType.minY + level.dimensionType.height) / Chunk.CHUNK_SECTION_SIZE
        val sectionSize = maxSection - minSection
        this.palette = IntArray(SECTION_BLOCKS_LENGTH * sectionSize)
    }

    @Synchronized
    internal fun registerChunk(level: Instance, chunk: Chunk) {
        for ((sectionIndex, section) in chunk.sections.withIndex()) {
            val sectionStart = SECTION_BLOCKS_LENGTH * sectionIndex
            for (x in 0..<Chunk.CHUNK_SECTION_SIZE) {
                for (y in 0..<Chunk.CHUNK_SECTION_SIZE) {
                    for (z in 0..<Chunk.CHUNK_SECTION_SIZE) {
                        val index = (y shl 8) or (z shl 4) or x
                        val block = section.blockPalette().get(x, y, z)
                        palette[sectionStart + index] = block
                    }
                }
            }
        }
        NativeBridge.registerChunk(level.getNativeID(), chunk.chunkX, chunk.chunkZ, palette)
    }
}