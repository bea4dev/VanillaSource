package com.github.bea4dev.vanilla_source

import com.github.bea4dev.vanilla_source.config.TomlConfig
import com.github.bea4dev.vanilla_source.config.server.ServerConfig
import com.github.bea4dev.vanilla_source.server.VanillaSource
import com.github.bea4dev.vanilla_source.util.unwrap
import java.io.File

fun main() {
    Resources.saveResource("server.toml", false)
    val serverConfig = TomlConfig.loadOrDefault<ServerConfig>(File("server.toml")).unwrap()

    println("Initializing console...")
    val console = Console()
    console.start()

    VanillaSource(serverConfig, console).start()
}

