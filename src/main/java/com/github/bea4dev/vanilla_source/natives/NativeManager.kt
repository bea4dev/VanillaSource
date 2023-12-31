package com.github.bea4dev.vanilla_source.natives

import com.github.bea4dev.vanilla_source.server.VanillaSource
import org.apache.commons.lang3.SystemUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicBoolean


private val initialized = AtomicBoolean(false)
private val logger = LoggerFactory.getLogger("NativeManager")

private const val LIBRARY_VERSION = 0

class NativeManager {
    private var enable = false

    init {
        if (initialized.getAndSet(true)) {
            VanillaSource.getServer().stopWithFatalError(IllegalStateException("Creating multiple NativeManagers is not allowed!"))
        }
    }

    fun init() {
        logger.info("Loading native libs...")
        val environment = getEnvironment()
        if (environment == null) {
            logger.warn("Unknown environment : ${System.getProperty("os.name")} ${System.getProperty("os.arch")}")
            return
        }

        val fileName = Paths.get("").toAbsolutePath().toString() + "/libs/" + environment.fileName
        if (!File(fileName).exists()) {
            logger.warn("Library file not found : /libs/${environment.fileName}")
            return
        }
        try {
            System.load(fileName)
        } catch (e: Error) {
            e.printStackTrace()
            logger.warn("Failed to load native library.")
            return
        }

        val version = NativeBridge.getVersion()
        if (version != LIBRARY_VERSION) {
            logger.warn("Version dose not match : Java = $LIBRARY_VERSION | Rust = $version")
            return
        }

        NativeBridge.createGlobalRegistry()

        enable = true
    }

    fun isEnabled(): Boolean {
        return this.enable
    }
}

enum class Environment(val fileName: String) {
    WINDOWS_X64("vanilla_source_windows_x64.dll"),
    LINUX_X64("libvanilla_source_linux_x64.so"),
    MACOS_X64("libvanilla_source_macos_x64.dylib"),
    MACOS_AARCH64("libvanilla_source_macos_aarch64.dylib")
}

fun getEnvironment(): Environment? {
    val cpu = System.getProperty("os.arch")
    if (cpu != "x86_64" && cpu != "amd64" && cpu != "aarch64") {
        return null
    }
    return if (cpu == "amd64" || cpu == "x86_64") {
        if (SystemUtils.IS_OS_WINDOWS) {
            Environment.WINDOWS_X64
        } else if (SystemUtils.IS_OS_LINUX) {
            Environment.LINUX_X64
        } else if (SystemUtils.IS_OS_MAC_OSX) {
            Environment.MACOS_X64
        } else {
            null
        }
    } else {
        if (SystemUtils.IS_OS_MAC_OSX) {
            Environment.MACOS_AARCH64
        } else {
            null
        }
    }
}