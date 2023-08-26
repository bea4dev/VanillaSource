package com.github.bea4dev.vanilla_source

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


class Resources {

    companion object {

        @JvmStatic
        fun saveResource(path: String, replace: Boolean): Result<Unit, Throwable> {
            val outPutPath = Path.of(path).fileName.toString()
            if (!replace && File(path).exists()) {
                return Ok(Unit)
            }

            return try {
                BufferedOutputStream(FileOutputStream(outPutPath)).use { output ->
                    Files.newInputStream(Paths.get(path)).use { input ->
                        input.transferTo(output)
                    }
                }
                Ok(Unit)
            } catch (err: Throwable) {
                Err(err)
            }
        }

    }

}