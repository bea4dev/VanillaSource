package com.github.bea4dev.vanilla_source.server.coroutine

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import net.minestom.server.timer.ExecutionType
import net.minestom.server.timer.Scheduler
import kotlin.coroutines.CoroutineContext

class SchedulerDispatcher(private val scheduler: Scheduler) : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        scheduler.scheduleNextProcess(block, ExecutionType.SYNC)
    }
}