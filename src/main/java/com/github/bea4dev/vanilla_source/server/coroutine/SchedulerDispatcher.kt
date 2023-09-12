package com.github.bea4dev.vanilla_source.server.coroutine

import com.github.bea4dev.vanilla_source.server.entity.VanillaSourceEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import net.minestom.server.entity.Entity
import net.minestom.server.timer.ExecutionType
import kotlin.coroutines.CoroutineContext

class SchedulerDispatcher(private val entity: Entity) : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        entity.scheduler().scheduleNextProcess(block, ExecutionType.SYNC)
    }

    override fun isDispatchNeeded(context: CoroutineContext): Boolean {
        if (this.entity is VanillaSourceEntity) {
            val thread = this.entity.getTickThread()
            if (thread != null && Thread.currentThread() == thread) {
                return false
            }
        }
        return true
    }
}