package com.github.bea4dev.vanilla_source.util.math

import kotlin.math.pow

class BezierCurve2D(var start: Vec2D, var startControl: Vec2D, var endControl: Vec2D, var end: Vec2D) {

    fun getPosition(t: Double): Vec2D {
        if (t !in 0.0..1.0) {
            throw IllegalArgumentException()
        }

        val t1 = 1 - t
        val x0 = start.x
        val x1 = startControl.x
        val x2 = endControl.x
        val x3 = end.x
        val y0 = start.y
        val y1 = startControl.y
        val y2 = endControl.y
        val y3 = end.y

        val xt = t1.pow(3)*x0 + 3*t1.pow(2)*t*x1 + 3*t1*t.pow(2)*x2 + t.pow(3)*x3
        val yt = t1.pow(3)*y0 + 3*t1.pow(2)*t*y1 + 3*t1*t.pow(2)*y2 + t.pow(3)*y3

        return Vec2D(xt, yt)
    }

}