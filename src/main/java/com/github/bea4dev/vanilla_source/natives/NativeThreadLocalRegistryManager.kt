package com.github.bea4dev.vanilla_source.natives

import com.github.bea4dev.vanilla_source.server.level.util.BlockPosition
import net.minestom.server.instance.Instance
import java.util.concurrent.ConcurrentHashMap

class NativeThreadLocalRegistryManager {

    companion object {
        private val levelMap = ConcurrentHashMap<Int, NativeThreadLocalRegistryManager>()

        fun getForLevelEntityThread(level: Instance?): NativeThreadLocalRegistryManager {
            return levelMap.computeIfAbsent(level.getNativeID()) { NativeThreadLocalRegistryManager() }
        }

        internal fun removeForLevelEntityThreadUnsafe(level: Instance) {
            levelMap[level.getNativeID()]?.deleteUnsafe()
            levelMap.remove(level.getNativeID())
        }
    }


    private val registryAddress = NativeBridge.createThreadLocalRegistry()

    private val argsArray = IntArray(9)
    private var returnArray = IntArray(200)
    private var thread: Thread? = null

    fun runPathfinding(levelId: Int, start: BlockPosition, goal: BlockPosition, descendingHeight: Int, jumpHeight: Int, maxIteration: Int): List<BlockPosition> {
        checkThread()

        val paths = mutableListOf<BlockPosition>()

        argsArray[0] = start.x
        argsArray[1] = start.y
        argsArray[2] = start.z
        argsArray[3] = goal.x
        argsArray[4] = goal.y
        argsArray[5] = goal.z
        argsArray[6] = descendingHeight
        argsArray[7] = jumpHeight
        argsArray[8] = maxIteration

        val result = NativeBridge.runPathfinding(registryAddress, levelId, argsArray, returnArray)
        for (i in 0..<result[0]) {
            val startIndex = i * 3 + 1
            paths += BlockPosition(result[startIndex], result[startIndex + 1], result[startIndex + 2])
        }
        this.returnArray = result

        return paths
    }

    fun releaseChunk(levelId: Int, chunkX: Int, chunkZ: Int) {
        checkThread()
        NativeBridge.unregisterChunk(registryAddress, levelId, chunkX, chunkZ)
    }

    fun deleteUnsafe() {
        checkThread()
        NativeBridge.removeThreadLocalRegistry(registryAddress)
    }

    private fun checkThread() {
        val thread = this.thread
        if (thread == null) {
            this.thread = Thread.currentThread()
        } else {
            if (thread != Thread.currentThread()) {
                throw IllegalStateException("It is not allowed to run this task in other thread.")
            }
        }
    }

}