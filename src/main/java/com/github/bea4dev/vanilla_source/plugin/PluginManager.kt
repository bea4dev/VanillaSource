package com.github.bea4dev.vanilla_source.plugin

import com.github.bea4dev.vanilla_source.server.VanillaSource
import com.google.common.reflect.ClassPath
import org.slf4j.LoggerFactory
import java.net.URLClassLoader
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.ArrayList

class PluginManager {
    private val plugins = ArrayList<VanillaSourcePlugin>()
    private val logger = LoggerFactory.getLogger("PluginManager")

    fun onEnable() {
        logger.info("Loading plugins...")

        loadPluginsInTree(Path.of("plugins"))
    }

    fun onDisable() {
        logger.info("Disabling plugins...")

        for (plugin in this.plugins) {
            plugin.onDisable()
        }
    }

    private fun loadPluginsInTree(root: Path) {
        val queue: Queue<Pair<Path, ClassLoader>> = LinkedList()
        queue.add(Pair(root, VanillaSource::class.java.classLoader))

        while (queue.isNotEmpty()) {
            val current = queue.poll()
            val currentPath = current.first
            val currentClassLoader = current.second

            if (Files.exists(currentPath) && Files.isDirectory(currentPath)) {
                val files = ArrayList<Path>()
                val dirs = ArrayList<Path>()

                Files.walkFileTree(
                    currentPath,
                    EnumSet.noneOf(FileVisitOption::class.java),
                    Int.MAX_VALUE,
                    object : SimpleFileVisitor<Path>() {
                        override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
                            if (file != null) {
                                files.add(file)
                            }
                            return FileVisitResult.CONTINUE
                        }

                        override fun preVisitDirectory(dir: Path?, attrs: BasicFileAttributes?): FileVisitResult {
                            if (dir != null && currentPath != dir) {
                                dirs.add(dir)
                            }
                            return FileVisitResult.CONTINUE
                        }
                    }
                )

                val uris = files.stream().map { file -> file.toUri().toURL() }.collect(Collectors.toList()).toTypedArray()
                val loader = URLClassLoader.newInstance(uris, currentClassLoader)

                val allPluginClasses = ClassPath.from(loader).topLevelClasses.stream()
                    .map { info -> try { info.load() } catch (_: NoClassDefFoundError) { null } }
                    .filter { clazz -> clazz?.isImplements(VanillaSourcePlugin::class.java)?: false }
                    .collect(Collectors.toSet())

                val nameSet = HashSet<String>()

                for (pluginClass in allPluginClasses) {
                    val pluginName = pluginClass!!.simpleName
                    if (nameSet.contains(pluginName)) {
                        throw IllegalStateException("Duplicated plugin name '${pluginName}'")
                    }
                    nameSet.add(pluginName)

                    val plugin = pluginClass.getConstructor().newInstance() as VanillaSourcePlugin
                    plugin.onEnable()
                    plugins.add(plugin)
                }

                for (dir in dirs) {
                    queue.add(Pair(dir, loader))
                }
            }
        }
    }

}


private fun Class<*>.isImplements(clazz: Class<*>): Boolean {
    for (impl in this.interfaces) {
        if (impl == clazz) { return true }
        if (impl.isImplements(clazz)) { return true }
    }
    return false
}