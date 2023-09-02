package com.github.bea4dev.vanilla_source.resource.model

import com.github.bea4dev.vanilla_source.util.math.BezierCurve2D
import com.github.bea4dev.vanilla_source.util.math.Vec2D
import team.unnamed.creative.base.Vector3Float

class AnimationEntry(val pos: Int, val value: Vector3Float)

@Suppress("DuplicatedCode")
fun bezier(entries: List<AnimationEntry>): MutableList<AnimationEntry> {
    val newAnimationEntries = mutableListOf<AnimationEntry>()

    var last: AnimationEntry? = null
    for (i in entries.indices) {
        if (i == entries.size - 1) {
            last = entries[i]
            break
        }

        val current = entries[i]
        val prev = if (i == 0) { AnimationEntry(current.pos - 1, current.value) } else { entries[i - 1] }
        val next = entries[i + 1]
        val nextOfNext = if (i + 2 == entries.size) { AnimationEntry(next.pos + 1, next.value) } else { entries[i + 2] }

        val xStart = Vec2D(current.pos.toDouble(), current.value.x().toDouble())
        val yStart = Vec2D(current.pos.toDouble(), current.value.y().toDouble())
        val zStart = Vec2D(current.pos.toDouble(), current.value.z().toDouble())
        val xEnd = Vec2D(next.pos.toDouble(), next.value.x().toDouble())
        val yEnd = Vec2D(next.pos.toDouble(), next.value.y().toDouble())
        val zEnd = Vec2D(next.pos.toDouble(), next.value.z().toDouble())

        val xPrev = Vec2D(prev.pos.toDouble(), prev.value.x().toDouble())
        val yPrev = Vec2D(prev.pos.toDouble(), prev.value.y().toDouble())
        val zPrev = Vec2D(prev.pos.toDouble(), prev.value.z().toDouble())
        val xNextNext = Vec2D(nextOfNext.pos.toDouble(), nextOfNext.value.x().toDouble())
        val yNextNext = Vec2D(nextOfNext.pos.toDouble(), nextOfNext.value.y().toDouble())
        val zNextNext = Vec2D(nextOfNext.pos.toDouble(), nextOfNext.value.z().toDouble())

        val xStartControl = xStart.add(xEnd.subtract(xPrev).normalize().multiply(3.0))
        val yStartControl = yStart.add(yEnd.subtract(yPrev).normalize().multiply(3.0))
        val zStartControl = zStart.add(zEnd.subtract(zPrev).normalize().multiply(3.0))

        val xEndControl = xEnd.add(xStart.subtract(xNextNext).normalize().multiply(3.0))
        val yEndControl = yEnd.add(yStart.subtract(yNextNext).normalize().multiply(3.0))
        val zEndControl = zEnd.add(zStart.subtract(zNextNext).normalize().multiply(3.0))

        val xCurve = BezierCurve2D(xStart, xStartControl, xEndControl, xEnd)
        val yCurve = BezierCurve2D(yStart, yStartControl, yEndControl, yEnd)
        val zCurve = BezierCurve2D(zStart, zStartControl, zEndControl, zEnd)

        fun interpolate(curve: BezierCurve2D): DoubleArray {
            var t = 0.0
            val start = current.pos
            var nextPosition = start
            val values = DoubleArray(next.pos - start)
            while (true) {
                if (t > 1.0 || nextPosition == next.pos) {
                    break
                }
                val curvePosition = curve.getPosition(t)
                val position = curvePosition.x.toInt()
                if (position == nextPosition) {
                    values[position - start] = curvePosition.y
                    nextPosition += 1
                }

                t += 0.0001
            }
            return values
        }

        val xValues = interpolate(xCurve)
        val yValues = interpolate(yCurve)
        val zValues = interpolate(zCurve)
        for (j in 0..<(next.pos - current.pos)) {
            val x = xValues[j]
            val y = yValues[j]
            val z = zValues[j]
            val position = current.pos + j
            if (position > 0) {
                newAnimationEntries += AnimationEntry(position, Vector3Float(x.toFloat(), y.toFloat(), z.toFloat()))
            }
        }
    }
    newAnimationEntries += last!!

    newAnimationEntries.sortWith(Comparator.comparingInt { entry -> entry.pos })

    return newAnimationEntries
}