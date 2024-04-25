package com.github.bea4dev.vanilla_source.server.entity.ai.astar

import com.github.bea4dev.vanilla_source.natives.NativeThreadLocalRegistryManager
import com.github.bea4dev.vanilla_source.natives.getNativeID
import com.github.bea4dev.vanilla_source.server.level.util.BlockPosition
import net.minestom.server.MinecraftServer
import net.minestom.server.instance.Instance
import net.minestom.server.timer.ExecutionType
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.collections.ArrayList
import kotlin.math.abs

private val EMPTY_LIST = ArrayList<BlockPosition>()

class AStarPathfinder(
    level: Instance?,
    val descendingHeight: Int,
    val jumpHeight: Int,
    val maxIteration: Int,
) {
    var start = BlockPosition(0, 0, 0)
    var goal = BlockPosition(0, 0, 0)
    private var level = WeakReference(level)

    //NodeData map
    private val nodeDataMap: MutableMap<BlockPosition, NodeData> = HashMap<BlockPosition, NodeData>()

    //Sorted node
    private val sortNodeSet: MutableSet<NodeData> = HashSet()

    private var levelId = level.getNativeID()
    private var nativeManager = NativeThreadLocalRegistryManager.getForLevelEntityThread(level)
    var useNative = true

    fun runPathfindingAsync(): CompletableFuture<List<BlockPosition>> {
        val future = if (useNative) {
            AsyncPathfinderThread.getThread().runPathfinding(levelId, start, goal, descendingHeight, jumpHeight, maxIteration)
        } else {
            // Start pathfinding at async
            val completableFuture = CompletableFuture<List<BlockPosition>>()
            MinecraftServer.getSchedulerManager().scheduleNextProcess({
                completableFuture.complete(
                    runPathFinding()
                )
            }, ExecutionType.TICK_START)
            completableFuture
        }
        return future
    }

    fun runPathFinding(): List<BlockPosition> {
        //Check the start and goal position.
        if (start == goal) {
            //I couldn't find a path...
            return EMPTY_LIST
        }

        val level = this.level.get() ?: return EMPTY_LIST

        val nativeManager = this.nativeManager
        if (useNative) {
            return nativeManager.runPathfinding(levelId, start, goal, descendingHeight, jumpHeight, maxIteration)
        }

        nodeDataMap.clear()
        sortNodeSet.clear()

        // Open first position node
        val startNode = openNode(null, start)
        startNode.isClosed = true

        var currentNode = startNode
        var nearestNode = currentNode

        var iteration = 0

        //Start pathfinding
        while (true) {
            iteration++

            //Max iteration check
            if (iteration >= maxIteration) {
                //Give up!
                val paths = ArrayList<BlockPosition>()
                paths.add(nearestNode.blockPosition)
                getPaths(nearestNode, paths)
                paths.reverse()


                /*
                for (position in paths) {
                    Audiences.players().forEachAudience { player -> (player as Player).sendPacket(BlockChangePacket(position.asPos(), Block.LIME_STAINED_GLASS)) }
                }*/
                return paths
            }

            //Audiences.players().forEachAudience { player -> (player as Player).sendPacket(BlockChangePacket(currentNode.blockPosition.asPos(), Block.GLASS)) }
            for (blockPosition in currentNode.getNeighbourBlockPosition(descendingHeight, jumpHeight, level)) {
                //Check if closed
                val newNode = openNode(currentNode, blockPosition)
                if (newNode.isClosed) continue

                //Update nearest node
                if (newNode.estimatedCost < nearestNode.estimatedCost) {
                    nearestNode = newNode
                }

                //Audiences.players().forEachAudience { player -> (player as Player).sendPacket(BlockChangePacket(newNode.blockPosition.asPos(), Block.GRAY_STAINED_GLASS)) }
                sortNodeSet.add(newNode)
            }

            //Close node
            currentNode.isClosed = true
            sortNodeSet.remove(currentNode)
            //Audiences.players().forEachAudience { player -> (player as Player).sendPacket(BlockChangePacket(currentNode.blockPosition.asPos(), Block.BLACK_STAINED_GLASS)) }
            if (sortNodeSet.size == 0) {
                //I couldn't find a path...
                val paths = ArrayList<BlockPosition>()
                paths.add(nearestNode.blockPosition)
                getPaths(nearestNode, paths)
                paths.reverse()

                /*
                for (position in paths) {
                    Audiences.players().forEachAudience { player -> (player as Player).sendPacket(BlockChangePacket(position.asPos(), Block.LIME_STAINED_GLASS)) }
                }*/
                return paths
            }

            //Choose next node
            var score = Int.MAX_VALUE
            for (nodeData in sortNodeSet) {
                if (nodeData.score < score) {
                    score = nodeData.score
                    currentNode = nodeData
                } else if (nodeData.score == score) {
                    if (nodeData.estimatedCost < currentNode.estimatedCost) {
                        currentNode = nodeData
                    } else if (nodeData.estimatedCost == currentNode.estimatedCost) {
                        if (nodeData.actualCost <= currentNode.actualCost) {
                            currentNode = nodeData
                        }
                    }
                }
            }

            //Check goal
            if (currentNode.blockPosition == goal) {
                val paths = ArrayList<BlockPosition>()
                paths.add(currentNode.blockPosition)
                getPaths(currentNode, paths)
                paths.reverse()

                /*
                for (position in paths) {
                    Audiences.players().forEachAudience { player -> (player as Player).sendPacket(BlockChangePacket(position.asPos(), Block.LIME_STAINED_GLASS)) }
                }*/
                return paths
            }
        }
    }

    private fun getPaths(nodeData: NodeData, paths: MutableList<BlockPosition>) {
        val origin = nodeData.origin ?: return
        paths.add(origin.blockPosition)
        getPaths(origin, paths)
    }

    private fun openNode(origin: NodeData?, blockPosition: BlockPosition): NodeData {
        val nodeData = nodeDataMap[blockPosition]
        if (nodeData != null) return nodeData

        //Calculate actual cost
        val actualCost = if (origin == null) 0 else origin.actualCost + 1

        //Calculate estimated cost
        val estimatedCost = abs(goal.x - blockPosition.x) + abs(goal.y - blockPosition.y) + abs(goal.z - blockPosition.z)
        return nodeDataMap.computeIfAbsent(blockPosition) { bp: BlockPosition ->
            NodeData(
                bp,
                origin,
                actualCost,
                estimatedCost
            )
        }
    }

    fun updateLevel(level: Instance) {
        this.level = WeakReference(level)
        this.levelId = level.getNativeID()
        this.nativeManager = NativeThreadLocalRegistryManager.getForLevelEntityThread(level)
    }

}

