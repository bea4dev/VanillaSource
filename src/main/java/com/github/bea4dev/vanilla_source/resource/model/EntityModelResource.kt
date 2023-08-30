package com.github.bea4dev.vanilla_source.resource.model

import com.github.bea4dev.vanilla_source.config.resource.EntityModelConfig
import com.github.bea4dev.vanilla_source.resource.FreezableResource
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.EntityType
import net.minestom.server.instance.Instance
import org.slf4j.LoggerFactory
import team.unnamed.creative.base.Vector3Float
import team.unnamed.creative.file.FileTree
import team.unnamed.hephaestus.Model
import team.unnamed.hephaestus.animation.Timeline
import team.unnamed.hephaestus.minestom.MinestomModelEngine
import team.unnamed.hephaestus.minestom.ModelEntity
import team.unnamed.hephaestus.reader.blockbench.BBModelReader
import team.unnamed.hephaestus.writer.ModelWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class EntityModelResource(private val config: EntityModelConfig) : FreezableResource() {

    companion object {
        private var instance: EntityModelResource? = null

        @JvmStatic
        fun createGlobalResource(config: EntityModelConfig) {
            val resource = EntityModelResource(config)
            resource.loadResources()
            resource.saveResources()
            resource.fixModels()
            resource.freeze()
            instance = resource
        }

        @JvmStatic
        fun getInstance(): EntityModelResource { return instance!! }

    }


    private val logger = LoggerFactory.getLogger(EntityModelResource::class.java)
    private val reader = BBModelReader.blockbench()
    // Thread-safety is achieved by freezing.
    private val models = HashMap<String, Model>()

    private fun loadResources() {
        assert()

        logger.info("Loading entity model resources...")

        val folder = File(config.settings.bbmodelsPath)
        if (folder.exists() && folder.isDirectory) {
            val files = folder.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.extension == "bbmodel") {
                        logger.info("Loading ${file.name}")
                        val model = reader.read(file)
                        models[model.name()] = model
                    } else {
                        logger.warn("Unknown file '${file.name}'.")
                    }
                }
            }
        } else {
            logger.warn("Not found directory '${config.settings.bbmodelsPath}'!")
        }
    }

    private fun saveResources() {
        assert()

        logger.info("Saving entity model resources...")

        val directory = File(config.settings.outputName)
        directory.mkdirs()

        FileTree.directory(directory).use { fileTree ->
            ModelWriter.resource("minecraft").write(fileTree, models.values)
        }

        try {
            val jsonFiles = findJsonFiles(File(config.settings.outputName))

            for (jsonFile in jsonFiles) {
                var content = jsonFile.readText()

                for (model in models.values) {
                    val searchText = model.name()
                    val replaceText = "item"

                    val regex = Regex("\"\\d+\"\\s*:\\s*\"${searchText}.*\"")
                    regex.findAll(content).forEach { result ->
                        val value = result.value
                        val valueReplaced = value.replace(searchText, replaceText)
                        content = content.replace(value, valueReplaced)
                    }
                }

                jsonFile.writeText(content)
                logger.info("Json file ${jsonFile.name} was replaced!")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val textureDirectory = File(directory.path + "/assets/minecraft/textures")
        val textureDirs = textureDirectory.listFiles()?.filter { dir -> dir.isDirectory }
        textureDirs?.forEach { file -> copyDirectory(file.path, "${textureDirectory}/item") }

        copyDirectory(config.settings.templatePath, config.settings.outputName)

        zipFolder(config.settings.outputName, config.settings.outputName + ".zip")
    }

    private fun fixModels() {
        for (model in models.values) {
            for (animation in model.animations().values) {
                for (timeLine in animation.timelines().values) {
                    val entriesField = timeLine.javaClass.getDeclaredField("entries")
                    entriesField.isAccessible = true

                    @Suppress("UNCHECKED_CAST")
                    val entryMap = entriesField.get(timeLine) as Map<Timeline.Channel, List<*>>
                    val animationEntries = entryMap[Timeline.Channel.ROTATION]!!

                    if (animationEntries.isEmpty()) {
                        continue
                    }

                    val newAnimationEntries = mutableListOf<AnimationEntry>()

                    var maxPosition = 0
                    for (animationEntry in animationEntries) {
                        val animationEntryClass = animationEntry!!.javaClass
                        val posField = animationEntryClass.getDeclaredField("pos")
                        val valueField = animationEntryClass.getDeclaredField("value")
                        posField.isAccessible = true
                        valueField.isAccessible = true

                        val pos = posField.getInt(animationEntry)
                        val value = valueField.get(animationEntry) as Vector3Float

                        newAnimationEntries += AnimationEntry(pos, value)

                        if (pos > maxPosition) {
                            maxPosition = pos
                        }
                    }

                    newAnimationEntries.sortWith(Comparator.comparingInt { entry -> entry.pos })

                    fun MutableList<AnimationEntry>.contains(position: Int): Boolean {
                        for (i in 0..<this.size) {
                            val entry = this[i]
                            if (entry.pos == position) {
                                return true
                            }
                        }
                        return false
                    }

                    if (newAnimationEntries.size > 2) {
                        val lastAnimationEntries = bezier(newAnimationEntries)
                        lastAnimationEntries.sortWith(Comparator.comparingInt { entry -> entry.pos })
                        for (animationEntry in lastAnimationEntries) {
                            if (!newAnimationEntries.contains(animationEntry.pos)) {
                                val position = animationEntry.pos
                                val value = animationEntry.value
                                timeLine.put(position, Timeline.Channel.ROTATION, value)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun findJsonFiles(directory: File): List<File> {
        val jsonFiles = mutableListOf<File>()
        val files = directory.listFiles()
        if (files != null) {
            for (file in files) {
                if (file.isDirectory) {
                    jsonFiles.addAll(findJsonFiles(file))
                } else if (file.isFile && file.extension.equals("json", ignoreCase = true)) {
                    jsonFiles.add(file)
                }
            }
        }
        return jsonFiles
    }

    private fun copyDirectory(sourceDirPath: String, destinationDirPath: String) {
        val sourcePath = Paths.get(sourceDirPath)
        val destinationPath = Paths.get(destinationDirPath)

        val options = setOf(
            FileVisitOption.FOLLOW_LINKS
        )

        Files.walkFileTree(sourcePath, options, Int.MAX_VALUE, object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                val targetDir = destinationPath.resolve(sourcePath.relativize(dir))
                if (!Files.exists(targetDir)) {
                    Files.createDirectory(targetDir)
                }
                return FileVisitResult.CONTINUE
            }

            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                val targetFile = destinationPath.resolve(sourcePath.relativize(file))
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING)
                return FileVisitResult.CONTINUE
            }
        })
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



    operator fun get(name: String): Model? {
        return models[name]
    }

    fun getModels(): Collection<Model> {
        return models.values
    }

    fun createModelEntity(name: String, level: Instance, position: Pos): ModelEntity {
        val model = this.get(name) ?: throw IllegalArgumentException("Model '${name}' is not found!")
        return MinestomModelEngine.minestom().createView(
            EntityType.ARMOR_STAND,
            model,
            MinestomModelEngine.BoneType.AREA_EFFECT_CLOUD,
            level,
            position
        )
    }

    fun createModelEntityTracked(name: String, level: Instance, position: Pos): ModelEntity {
        val view = createModelEntity(name, level, position)
        MinestomModelEngine.minestom().tracker().startGlobalTracking(view)
        return view
    }

}