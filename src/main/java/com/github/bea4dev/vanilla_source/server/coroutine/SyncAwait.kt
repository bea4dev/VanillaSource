package com.github.bea4dev.vanilla_source.server.coroutine

import com.github.bea4dev.vanilla_source.server.VanillaSource
import com.github.shynixn.mccoroutine.minestom.launch
import com.github.shynixn.mccoroutine.minestom.scope
import kotlinx.coroutines.*
import net.minestom.server.entity.Entity

fun Entity.launch(block: suspend CoroutineScope.() -> Unit): Job {
    val dispatcher = SchedulerDispatcher(this)
    return VanillaSource.getMinecraftServer().scope.launch(dispatcher, CoroutineStart.DEFAULT, block)
}

fun <T> Entity.sync(block: suspend CoroutineScope.() -> T): Deferred<T> {
    val dispatcher = SchedulerDispatcher(this)
    return VanillaSource.getMinecraftServer().scope.async(dispatcher, CoroutineStart.DEFAULT, block)
}

fun launch(block: suspend CoroutineScope.() -> Unit): Job {
    return VanillaSource.getMinecraftServer().launch(block = block)
}

val Int.tick: Long
    get() = this * 50L