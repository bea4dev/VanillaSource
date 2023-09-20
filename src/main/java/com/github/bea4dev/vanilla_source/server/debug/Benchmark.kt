package com.github.bea4dev.vanilla_source.server.debug

import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.adventure.audience.Audiences
import net.minestom.server.event.server.ServerTickMonitorEvent
import net.minestom.server.monitoring.TickMonitor
import net.minestom.server.utils.MathUtils
import net.minestom.server.utils.time.TimeUnit
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference


private val LAST_TICK = AtomicReference<TickMonitor>()

fun registerBenchmarkTask() {
    MinecraftServer.getGlobalEventHandler().addListener(ServerTickMonitorEvent::class.java) { event ->
        LAST_TICK.set(event.tickMonitor)
    }

    val benchmarkManager = MinecraftServer.getBenchmarkManager()
    benchmarkManager.enable(Duration.of(10, TimeUnit.SECOND))
    MinecraftServer.getSchedulerManager().buildTask {
        val players = MinecraftServer.getConnectionManager().onlinePlayers
        if (players.isEmpty()) return@buildTask

        var ramUsage = benchmarkManager.usedMemory
        ramUsage = (ramUsage / 1e6).toLong()

        val tickMonitor: TickMonitor = LAST_TICK.get()
        val header: Component =
            Component.text("RAM USAGE: $ramUsage MB")
                .append(Component.newline())
                .append(
                    Component.text(
                        "TICK TIME: " + MathUtils.round(
                            tickMonitor.tickTime,
                            2
                        ) + "ms"
                    )
                )
                .append(Component.newline())
                .append(
                    Component.text(
                        "ACQ TIME: " + MathUtils.round(
                            tickMonitor.acquisitionTime,
                            2
                        ) + "ms"
                    )
                )
        val footer = benchmarkManager.cpuMonitoringMessage
        Audiences.players().sendPlayerListHeaderAndFooter(header, footer)
    }.repeat(10, TimeUnit.SERVER_TICK).schedule()
}