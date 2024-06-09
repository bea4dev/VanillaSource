package com.github.bea4dev.vanilla_source.config

import cc.ekblad.toml.TomlMapper
import cc.ekblad.toml.decode
import cc.ekblad.toml.tomlMapper
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import java.io.File
import java.nio.charset.StandardCharsets
import kotlin.reflect.full.companionObjectInstance

interface DefaultTomlConfig: TomlConfig {
    fun default(): TomlConfig
    fun mapper(): TomlMapper
}

interface TomlConfig {

    companion object {
        inline fun <reified T> hasNoDefaultValue(): Throwable {
            return IllegalArgumentException("Type '${T::class.simpleName}' has no default value.")
        }

        inline fun <reified T: TomlConfig> defaultUnit(): DefaultTomlConfig? {
            val companionObject = T::class.companionObjectInstance
            return if (companionObject is DefaultTomlConfig) {
                companionObject
            } else {
                null
            }
        }

        inline fun <reified T: TomlConfig> default(): Result<T, Throwable> {
            val unit = defaultUnit<T>() ?: return Err(hasNoDefaultValue<T>())
            val default = unit.default()
            return Ok(default as T)
        }

        inline fun <reified T: TomlConfig> load(file: File): Result<T, Throwable> {
            return try {
                val str = file.readText(StandardCharsets.UTF_8)
                this.decode<T>(str)
            } catch (err: Throwable) {
                Err(err)
            }
        }

        inline fun <reified T: TomlConfig> decode(str: String): Result<T, Throwable> {
            return try {
                val unit = defaultUnit<T>()
                val mapper = unit?.mapper() ?: tomlMapper { /*None*/ }
                Ok(mapper.decode<T>(str))
            } catch (err: Throwable) {
                Err(err)
            }
        }

        inline fun <reified T: TomlConfig> loadOrDefault(file: File): Result<T, Throwable> {
            return if (file.exists()) {
                load<T>(file)
            } else {
                default<T>()
            }
        }

        inline fun <reified T: TomlConfig> decodeOrDefault(str: String?): Result<T, Throwable> {
            return if (str == null) {
                default<T>()
            } else {
                decode<T>(str)
            }
        }
    }

}