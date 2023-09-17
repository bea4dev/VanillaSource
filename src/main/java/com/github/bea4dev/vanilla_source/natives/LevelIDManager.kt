package com.github.bea4dev.vanilla_source.natives

import net.minestom.server.instance.Instance
import java.util.WeakHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class LevelIDManager {

    companion object {
        private val idMap = WeakHashMap<Instance, Int>()
        private var id = 0
        private val lock = ReentrantLock()

        @JvmStatic
        fun getId(level: Instance): Int {
            return lock.withLock {
                idMap.computeIfAbsent(level) { id++ }
            }
        }
    }

}


fun Instance.getNativeID(): Int {
    return LevelIDManager.getId(this)
}