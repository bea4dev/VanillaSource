package com.github.bea4dev.vanilla_source.server.entity.ai.astar

import com.github.bea4dev.vanilla_source.server.level.util.BlockPosition
import net.minestom.server.instance.Instance


class NodeData(
    val blockPosition: BlockPosition,
    val origin: NodeData?,
    val actualCost: Int,
    val estimatedCost: Int,
    val score: Int = actualCost + estimatedCost
) {

    var isClosed = false

    fun getNeighbourBlockPosition(down: Int, up: Int, level: Instance, ): Set<BlockPosition> {
        val neighbour = HashSet<BlockPosition>()

        val p1 = addIfCanStand(neighbour, BlockPosition(blockPosition.x + 1, blockPosition.y, blockPosition.z), level)
        val p2 = addIfCanStand(neighbour, BlockPosition(blockPosition.x, blockPosition.y, blockPosition.z + 1), level)
        val p3 = addIfCanStand(neighbour, BlockPosition(blockPosition.x - 1, blockPosition.y, blockPosition.z), level)
        val p4 = addIfCanStand(neighbour, BlockPosition(blockPosition.x, blockPosition.y, blockPosition.z - 1), level)
        if (p1 || p2) {
            addIfCanStand(neighbour, BlockPosition(blockPosition.x + 1, blockPosition.y, blockPosition.z + 1), level)
        }
        if (p2 || p3) {
            addIfCanStand(neighbour, BlockPosition(blockPosition.x - 1, blockPosition.y, blockPosition.z + 1), level)
        }
        if (p3 || p4) {
            addIfCanStand(neighbour, BlockPosition(blockPosition.x - 1, blockPosition.y, blockPosition.z - 1), level)
        }
        if (p4 || p1) {
            addIfCanStand(neighbour, BlockPosition(blockPosition.x + 1, blockPosition.y, blockPosition.z - 1), level)
        }
        for (uh in 1..up) {
            if (!isTraversable(level, blockPosition.x, blockPosition.y + uh, blockPosition.z)) {
                break
            }
            addIfCanStand(neighbour, BlockPosition(blockPosition.x + 1, blockPosition.y + uh, blockPosition.z), level)
            addIfCanStand(neighbour, BlockPosition(blockPosition.x, blockPosition.y + uh, blockPosition.z + 1), level)
            addIfCanStand(neighbour, BlockPosition(blockPosition.x - 1, blockPosition.y + uh, blockPosition.z), level)
            addIfCanStand(neighbour, BlockPosition(blockPosition.x, blockPosition.y + uh, blockPosition.z - 1), level)
        }

        var da = true
        var db = true
        var dc = true
        var dd = true
        for (dy in 1..down) {
            if (da) {
                if (isTraversable(level, blockPosition.x + 1, blockPosition.y - dy + 1, blockPosition.z)) {
                    addIfCanStand(neighbour, BlockPosition(blockPosition.x + 1, blockPosition.y - dy, blockPosition.z), level)
                } else {
                    da = false
                }
            }
            if (db) {
                if (isTraversable(level, blockPosition.x, blockPosition.y - dy + 1, blockPosition.z + 1)) {
                    addIfCanStand(neighbour, BlockPosition(blockPosition.x, blockPosition.y - dy, blockPosition.z + 1), level)
                } else {
                    db = false
                }
            }
            if (dc) {
                if (isTraversable(level, blockPosition.x - 1, blockPosition.y - dy + 1, blockPosition.z)) {
                    addIfCanStand(neighbour, BlockPosition(blockPosition.x - 1, blockPosition.y - dy, blockPosition.z), level)
                } else {
                    dc = false
                }
            }
            if (dd) {
                if (isTraversable(level, blockPosition.x, blockPosition.y - dy + 1, blockPosition.z - 1)) {
                    addIfCanStand(neighbour, BlockPosition(blockPosition.x, blockPosition.y - dy, blockPosition.z - 1), level)
                } else {
                    dd = false
                }
            }
        }
        return neighbour
    }

    private fun addIfCanStand(positions: MutableCollection<BlockPosition>, position: BlockPosition, world: Instance): Boolean {
        if (canStand(world, position.x, position.y, position.z)) {
            positions.add(position)
            return true
        }
        return false
    }
}


fun canStand(level: Instance, blockX: Int, blockY: Int, blockZ: Int): Boolean {
    val chunk = level.getChunkAt(blockX.toDouble(), blockZ.toDouble())!!

    val block1 = chunk.getBlock(blockX, blockY, blockZ)
    val block2 = chunk.getBlock(blockX, blockY + 1, blockZ)
    val block3 = chunk.getBlock(blockX, blockY - 1, blockZ)

    return !block1.isSolid && !block2.isSolid && block3.isSolid
}

fun isTraversable(level: Instance, blockX: Int, blockY: Int, blockZ: Int): Boolean {
    val chunk = level.getChunkAt(blockX.toDouble(), blockZ.toDouble())!!

    val block1 = chunk.getBlock(blockX, blockY, blockZ)
    val block2 = chunk.getBlock(blockX, blockY + 1, blockZ)

    return !block1.isSolid && !block2.isSolid
}
