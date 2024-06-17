package com.github.bea4dev.vanilla_source.resource.model

import com.github.bea4dev.vanilla_source.resource.FreezableResource
import net.worldseed.multipart.ModelEngine
import net.worldseed.resourcepack.PackBuilder
import org.apache.commons.io.FileUtils
import java.io.*
import java.nio.charset.Charset
import java.util.Arrays
import java.util.stream.Collectors
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.Path

private const val OUTPUT_PATH = "pack/resource-pack"
private const val TEMPLATE_PATH = "pack/resource-pack-template"
private const val BB_MODELS_PATH = "pack/bbmodels"
private const val MODELS_TEMPLATE_PATH = "pack/models"
private const val MODEL_MAPPING_JSON_PATH = "pack/model_mappings.json"
private const val ZIP_FILE_PATH = "pack/resource-pack.zip"

object EntityModelResources: FreezableResource() {
    private lateinit var models: List<String>

    fun loadModels() {
        File(OUTPUT_PATH).delete()
        FileUtils.copyDirectory(File(TEMPLATE_PATH), File(OUTPUT_PATH))

        val modelFiles = File(BB_MODELS_PATH).listFiles { file -> file.extension == "bbmodel" } ?: arrayOf()
        models = Arrays.stream(modelFiles).map { file -> file.nameWithoutExtension }.collect(Collectors.toList())

        val generateConfig = PackBuilder.Generate(Path(BB_MODELS_PATH), Path(OUTPUT_PATH), Path(MODELS_TEMPLATE_PATH))
        FileUtils.writeStringToFile(File(MODEL_MAPPING_JSON_PATH), generateConfig.modelMappings(), Charset.defaultCharset())

        val mappingsData = InputStreamReader(FileInputStream(File(MODEL_MAPPING_JSON_PATH)))
        ModelEngine.loadMappings(mappingsData, Path(MODELS_TEMPLATE_PATH))

        zipFolder(OUTPUT_PATH, ZIP_FILE_PATH)

        super.freeze()
    }

    fun models(): List<String> {
        return models
    }

}


private fun zipFolder(folderPath: String, zipFilePath: String) {
    val sourceFolder = File(folderPath)
    val outputStream = FileOutputStream(zipFilePath)
    val zipStream = ZipOutputStream(outputStream)

    fun addToZip(sourceFile: File, entryPath: String) {
        if (sourceFile.isFile) {
            val fileInputStream = FileInputStream(sourceFile)
            val zipEntry = ZipEntry(entryPath)

            zipStream.putNextEntry(zipEntry)

            val buffer = ByteArray(1024)
            var length: Int
            while (fileInputStream.read(buffer).also { length = it } > 0) {
                zipStream.write(buffer, 0, length)
            }

            fileInputStream.close()
            zipStream.closeEntry()
        } else if (sourceFile.isDirectory) {
            sourceFile.listFiles()?.forEach { file ->
                val newEntryPath = if (entryPath.isNotEmpty()) "$entryPath/${file.name}" else file.name
                addToZip(file, newEntryPath)
            }
        }
    }

    addToZip(sourceFolder, "")

    zipStream.close()
    outputStream.close()
}