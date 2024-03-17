package com.github.bea4dev.vanilla_source.lang

import com.github.bea4dev.vanilla_source.Resources
import com.moandjiezana.toml.Toml
import java.io.File

object LanguageText {
    private val map: Map<String, Toml>

    init {
        val fileName = "lang/ja_JP.toml"
        val file = File(fileName)
        val dir = File("lang")

        if (!file.exists()) {
            dir.mkdir()
            Resources.saveResource(fileName, false)
        }

        val map = mutableMapOf<String, Toml>()
        for (langFile in dir.listFiles()!!) {
            if (langFile.extension != "toml") {
                continue
            }

            val langName = langFile.nameWithoutExtension
            map[langName] = Toml().read(langFile)
        }

        this.map = map
    }

    fun getText(lang: String, key: String): String {
        val value = map[lang]?.let { toml -> getPathValue(toml, key.split(".")) }
            ?: "$lang | $key : Unknown text!"
        return value as String
    }

    private fun getPathValue(toml: Toml, pathComponents: List<String>): Any? {
        var current: Any? = toml
        for (component in pathComponents) {
            current = when (current) {
                is Toml -> current.toMap()[component]
                is Map<*, *> -> current[component]
                else -> return null
            }
        }
        return current
    }
}