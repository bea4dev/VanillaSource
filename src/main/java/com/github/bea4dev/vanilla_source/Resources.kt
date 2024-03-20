package com.github.bea4dev.vanilla_source

import com.github.bea4dev.vanilla_source.plugin.VanillaSourcePlugin
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URLConnection


class Resources {

    companion object {

        @JvmStatic
        fun saveResource(path: String, replace: Boolean, loader: ClassLoader): Result<Unit, Throwable> {
            val outputFile = File(path)
            if (!replace && outputFile.exists()) {
                return Ok(Unit)
            }

            val url = loader.getResource(path) ?: return Err(NoSuchFileException(File(path)))
            val connection: URLConnection = url.openConnection()
            connection.useCaches = false
            val inputStream = connection.getInputStream()

            return try {
                BufferedOutputStream(FileOutputStream(outputFile)).use { output ->
                    inputStream.use { input ->
                        input.transferTo(output)
                    }
                }
                Ok(Unit)
            } catch (err: Throwable) {
                Err(err)
            }
        }

        @JvmStatic
        fun saveResource(path: String, replace: Boolean): Result<Unit, Throwable> {
            return saveResource(path, replace, ClassLoader.getSystemClassLoader())
        }

        @JvmStatic
        fun <T: VanillaSourcePlugin> savePluginResource(
            path: String,
            replace: Boolean,
            pluginClass: Class<T>
        ): Result<Unit, Throwable> {
            return saveResource(path, replace, pluginClass.classLoader)
        }

    }

}