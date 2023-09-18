package com.github.bea4dev.vanilla_source.natives

import com.github.bea4dev.vanilla_source.server.level.util.BlockPosition

class NativeThreadLocalRegistryManager {
    private val registryAddress = NativeBridge.createThreadLocalRegistry()

    private val argsArray = IntArray(9)
    private var returnArray = IntArray(200)
    private val cachedPathList = mutableListOf<BlockPosition>()

    fun runPathfinding(levelId: Int, start: BlockPosition, goal: BlockPosition, descendingHeight: Int, jumpHeight: Int, maxIteration: Int): List<BlockPosition> {
        val paths = this.cachedPathList
        paths.clear()

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

    fun deleteUnsafe() {
        NativeBridge.removeThreadLocalRegistry(registryAddress)
    }

}