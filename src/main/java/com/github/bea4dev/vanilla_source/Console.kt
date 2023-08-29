package com.github.bea4dev.vanilla_source

import com.github.bea4dev.vanilla_source.server.VanillaSource
import net.minecrell.terminalconsole.SimpleTerminalConsole
import net.minecrell.terminalconsole.TerminalConsoleAppender
import net.minestom.server.MinecraftServer
import org.jline.terminal.Terminal
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

class Console: SimpleTerminalConsole() {
    private val isRunning = AtomicBoolean(true)
    private val logger = LoggerFactory.getLogger("Console")!!
    private val thread = Thread { super.start() }

    init {
        // Initialize TerminalConsoleAppender
        TerminalConsoleAppender.isAnsiSupported()
    }

    override fun start() {
        thread.isDaemon = true
        thread.start()
    }

    fun stop() {
        if (isRunning.getAndSet(false)) {
            thread.interrupt()
        }
    }

    override fun isRunning(): Boolean {
        return isRunning.get()
    }

    override fun runCommand(command: String?) {
        if (command != null) {
            logger.info("/$command")
            MinecraftServer.getCommandManager().executeServerCommand(command)
        }
    }

    override fun shutdown() {
        VanillaSource.getServer().stop()
    }
}