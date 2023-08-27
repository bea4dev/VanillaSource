package com.github.bea4dev.vanilla_source.util.math

import kotlin.math.pow
import kotlin.math.sqrt

class Vec2D(val x: Double, val y: Double) {

    fun distance(other: Vec2D): Double {
        val x = (this.x - other.x).pow(2)
        val y = (this.y - other.y).pow(2)
        return sqrt(x + y)
    }

    fun length(): Double {
        return sqrt(this.x.pow(2) + this.y.pow(2))
    }

    fun normalize(): Vec2D {
        val length = this.length()
        return Vec2D(this.x / length, this.y / length)
    }

    fun multiply(factor: Double): Vec2D {
        return Vec2D(this.x * factor, this.y * factor)
    }

    fun add(other: Vec2D): Vec2D {
        return Vec2D(this.x + other.x, this.y + other.y)
    }

    fun subtract(other: Vec2D): Vec2D {
        return Vec2D(this.x - other.x, this.y - other.y)
    }

}