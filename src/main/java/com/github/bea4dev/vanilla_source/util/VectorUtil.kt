package com.github.bea4dev.vanilla_source.util

import net.minestom.server.coordinate.Vec

fun Vec.clone(): Vec {
    return Vec(this.x, this.y, this.z)
}