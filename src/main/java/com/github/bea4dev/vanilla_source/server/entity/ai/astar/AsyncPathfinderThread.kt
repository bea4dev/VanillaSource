package com.github.bea4dev.vanilla_source.server.entity.ai.astar

import com.github.bea4dev.vanilla_source.natives.NativeThreadLocalRegistryManager
import com.github.bea4dev.vanilla_source.server.level.util.BlockPosition
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class AsyncPathfinderThread private constructor() {

    companion object {
        private val threads = CopyOnWriteArrayList<AsyncPathfinderThread>()
        private var cursor = 0

        fun getAllThreads(): Collection<AsyncPathfinderThread> {
            return threads
        }

        fun getThread(): AsyncPathfinderThread {
            cursor++
            return threads[cursor % threads.size]
        }

        fun shutdownAll() {
            for (thread in threads) {
                thread.thread.shutdownNow()
            }
        }


        private val initialized = AtomicBoolean(false)

        fun initialize(numberOfThreads: Int) {
            if (initialized.getAndSet(true)) {
                throw IllegalStateException("It has already initialized!")
            }
            for (i in 0..<numberOfThreads) {
                threads += AsyncPathfinderThread()
            }
        }
    }


    private val thread = Executors.newSingleThreadExecutor()
    private val registry = NativeThreadLocalRegistryManager()

    fun runPathfinding(levelId: Int, start: BlockPosition, goal: BlockPosition, descendingHeight: Int, jumpHeight: Int, maxIteration: Int): CompletableFuture<List<BlockPosition>> {
        val future = CompletableFuture<List<BlockPosition>>()
        this.thread.execute {
            val result = registry.runPathfinding(levelId, start, goal, descendingHeight, jumpHeight, maxIteration)
            future.complete(result)
        }
        return future
    }

    fun releaseChunk(levelId: Int, chunkX: Int, chunkZ: Int) {
        this.thread.execute { registry.releaseChunk(levelId, chunkX, chunkZ) }
    }
}