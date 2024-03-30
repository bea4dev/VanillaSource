package com.github.bea4dev.vanilla_source.lang

import com.github.bea4dev.vanilla_source.Resources
import com.moandjiezana.toml.Toml
import net.kyori.adventure.key.Key
import net.kyori.adventure.translation.GlobalTranslator
import net.kyori.adventure.translation.TranslationRegistry
import net.kyori.adventure.translation.Translator
import java.io.File
import java.text.MessageFormat
import java.util.Locale

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

    fun initialize() {
        val registry = TranslationRegistry.create(Key.key("vanilla_source", "translate"))
        for (entry in map.entries) {
            val lang = entry.key
            val toml = entry.value
            val locale = Translator.parseLocale(lang) ?: throw IllegalStateException("Unknown locale : $lang")
            registerGlobalTranslator(registry, locale, toml, "")
        }
        GlobalTranslator.translator().addSource(registry)
    }

    private fun registerGlobalTranslator(registry: TranslationRegistry, locale: Locale, toml: Any, key: String) {
        if (toml is String) {
            registry.register(key, locale, MessageFormat(toml))
        }

        val entries = when (toml) {
            is Toml -> toml.toMap().entries
            is Map<*, *> -> toml.entries
            else -> return
        }

        val keyTemp = if (key.isEmpty()) { key } else { "$key." }
        entries.forEach { entry -> registerGlobalTranslator(registry, locale, entry.value, keyTemp + entry.key) }
    }

    fun getText(lang: String, key: String, vararg args: String): String {
        val text = map[lang]?.let { toml -> getPathValue(toml, key) as? String } ?: "$lang | $key : Unknown text!"
        return text.format(args)
    }

    fun getTextOrNull(lang: String, key: String, vararg args: String): String? {
        val text = map[lang]?.let { toml -> getPathValue(toml, key) as? String }
        return text?.format(args)
    }

    private fun getPathValue(toml: Toml, path: String): Any? {
        var current: Any? = toml
        for (component in path.split(".")) {
            current = when (current) {
                is Toml -> current.toMap()[component]
                is Map<*, *> -> current[component]
                else -> return null
            }
        }
        return current
    }
}