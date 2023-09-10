package com.github.bea4dev.vanilla_source.util.math

fun normalizeDegrees(degrees: Float): Float {
    var temp = degrees
    temp %= 360.0f
    if (temp < -180.0f){
        temp += 360.0f
    }
    if (temp > 180.0f){
        temp -= 360.0f
    }
    return temp
}