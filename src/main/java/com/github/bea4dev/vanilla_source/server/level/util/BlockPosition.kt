package com.github.bea4dev.vanilla_source.server.level.util

import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import java.util.*

fun <T: Point> T.asBlockPosition(): BlockPosition {
    return BlockPosition(this.blockX(), this.blockY(), this.blockZ())
}

class BlockPosition(val x: Int, val y: Int, val z: Int) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as BlockPosition
        return x == that.x && y == that.y && z == that.z
    }

    override fun hashCode(): Int {
        return Objects.hash(x, y, z)
    }

    fun asPos(): Pos {
        return Pos(x.toDouble(), y.toDouble(), z.toDouble())
    }

    fun asVec(): Vec {
        return Vec(x.toDouble(), y.toDouble(), z.toDouble())
    }
}
