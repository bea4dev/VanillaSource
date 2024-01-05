package com.github.bea4dev.vanilla_source.plugin

import com.google.common.reflect.ClassPath
import org.slf4j.LoggerFactory
import java.net.URLClassLoader
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import kotlin.io.path.Path

class PluginManager {
    private val plugins = ArrayList<VanillaSourcePlugin>()
    private val logger = LoggerFactory.getLogger("PluginManager")

    fun onEnable() {
        logger.info("Loading plugins...")

        for (file in listFiles(Path("plugins"))) {
            val loader = URLClassLoader.newInstance(arrayOf(file.toUri().toURL()))

            val allPluginClasses = ClassPath.from(loader).topLevelClasses.stream()
                .map { info -> info.load() }
                .filter { clazz -> clazz.isImplements(VanillaSourcePlugin::class.java) }
                .collect(Collectors.toSet())

            val nameSet = HashSet<String>()

            for (pluginClass in allPluginClasses) {
                val pluginName = pluginClass.simpleName
                if (nameSet.contains(pluginName)) {
                    throw IllegalStateException("Duplicated plugin name '${pluginName}'")
                }
                nameSet.add(pluginName)

                val plugin = pluginClass.getConstructor().newInstance() as VanillaSourcePlugin
                plugin.onEnable()
                this.plugins.add(plugin)
            }
        }
    }

    fun onDisable() {
        logger.info("Disabling plugins...")

        for (plugin in this.plugins) {
            plugin.onDisable()
        }
    }

    private fun listFiles(root: Path): List<Path> {
        val result: MutableList<Path> = mutableListOf()

        val queue: Queue<Path> = LinkedList()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val currentPath = queue.poll()

            if (Files.isDirectory(currentPath)) {
                try {
                    Files.walkFileTree(
                        currentPath,
                        EnumSet.noneOf(FileVisitOption::class.java),
                        Int.MAX_VALUE,
                        object : SimpleFileVisitor<Path>() {
                            override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
                                if (file != null) {
                                    result.add(file)
                                }
                                return FileVisitResult.CONTINUE
                            }

                            override fun preVisitDirectory(dir: Path?, attrs: BasicFileAttributes?): FileVisitResult {
                                if (dir != null) {
                                    queue.add(dir)
                                }
                                return FileVisitResult.CONTINUE
                            }
                        }
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        return result
    }

}


private fun Class<*>.isImplements(clazz: Class<*>): Boolean {
    for (impl in this.interfaces) {
        if (impl == clazz) { return true }
        if (impl.isImplements(clazz)) { return true }
    }
    return false
}