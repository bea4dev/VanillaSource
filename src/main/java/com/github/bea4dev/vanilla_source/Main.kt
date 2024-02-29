package com.github.bea4dev.vanilla_source

import com.github.bea4dev.vanilla_source.config.TomlConfig
import com.github.bea4dev.vanilla_source.config.server.ServerConfig
import com.github.bea4dev.vanilla_source.server.VanillaSource
import com.github.michaelbull.result.unwrap
import java.io.File

fun main() {
    // Console
    println("Initializing a console...")
    val console = Console()
    console.start()

    // Server settings
    Resources.saveResource("server.toml", false)
    val serverConfig = TomlConfig.loadOrDefault<ServerConfig>(File("server.toml")).unwrap()

    // Start server
    VanillaSource(serverConfig, console).start()
}

