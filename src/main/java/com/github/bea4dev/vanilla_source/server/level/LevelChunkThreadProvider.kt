package com.github.bea4dev.vanilla_source.server.level

import net.minestom.server.instance.Chunk
import net.minestom.server.instance.Instance
import net.minestom.server.instance.SharedInstance
import net.minestom.server.thread.ThreadProvider
import java.util.WeakHashMap

@Suppress("UnstableApiUsage")
class LevelChunkThreadProvider(private val threadCount: Int) : ThreadProvider<Chunk> {
    private var threadMap = WeakHashMap<Instance, Int>()
    private var count = 0

    override fun findThread(partition: Chunk): Int {
        val level = when (val level = partition.instance) {
            is SharedInstance -> level.instanceContainer
            else -> level
        }
        return threadMap[level] ?: addLevel(level)
    }

    @Synchronized
    private fun addLevel(level: Instance): Int {
        val index = threadMap[level]
        if (index != null) {
            return index
        }
        // Copy on write
        val clone = WeakHashMap<Instance, Int>()
        clone.putAll(threadMap)
        val newIndex = count++ % threadCount
        clone[level] = newIndex
        threadMap = clone
        return newIndex
    }
}