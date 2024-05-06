package com.github.bea4dev.vanilla_source.server.level.block

import net.minestom.server.coordinate.Point
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block

var GLOBAL_DIGGING_HANDLER: DiggingHandler? = null

interface DiggingHandler {

    fun getDiggingTime(level: Instance, blockPosition: Point, block: Block, player: Player): Int?

}