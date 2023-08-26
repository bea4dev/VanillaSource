package com.github.bea4dev.vanilla_source.server.level.generator

import com.github.bea4dev.vanilla_source.server.level.generator.debug.DebugGenerator
import net.minestom.server.instance.generator.Generator
import java.util.concurrent.ConcurrentHashMap

class GeneratorRegistry {
    companion object {
        private val generatorMap = ConcurrentHashMap<String, Generator>()

        fun init() {
            register("debug", DebugGenerator())
        }

        @JvmStatic
        fun getGenerator(name: String): Generator? {
            return generatorMap[name]
        }

        @JvmStatic
        fun register(name: String, generator: Generator) {
            generatorMap[name] = generator
        }
    }
}