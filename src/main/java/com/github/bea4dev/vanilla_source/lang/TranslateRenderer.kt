package com.github.bea4dev.vanilla_source.lang

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.TranslationArgument
import net.kyori.adventure.text.renderer.TranslatableComponentRenderer
import net.kyori.adventure.translation.GlobalTranslator
import java.text.MessageFormat
import java.util.*

object TranslateRenderer: TranslatableComponentRenderer<Locale>() {

    override fun translate(key: String, context: Locale): MessageFormat? {
        return GlobalTranslator.translator().translate(key, context)
    }

    override fun renderTranslatable(component: TranslatableComponent, context: Locale): Component {
        val format = super.translate(component.key(), component.fallback(), context)
        if (format != null) {
            val args = component.arguments()
            val builder = Component.text()
            super.mergeStyle(component, builder, context)
            if (args.isEmpty()) {
                builder.content(format.format(null, StringBuffer(), null).toString())
                return this.optionallyRenderChildrenAppendAndBuild(component.children(), builder, context)
            } else {
                val split = format.toPattern().split("%s")
                builder.append(Component.text(split[0]))

                for (i in 1 until split.size) {
                    args.getOrNull(i - 1)?.let { arg ->
                        if (arg.value() is Component) {
                            builder.append(this.render(arg.asComponent(), context))
                        } else {
                            builder.append(arg.asComponent())
                        }
                    }

                    builder.append(Component.text(split[i]))
                }

                return this.optionallyRenderChildrenAppendAndBuild(component.children(), builder, context)
            }
        } else {
            val builder = Component.translatable().key(component.key()).fallback(component.fallback())
            if (component.arguments().isNotEmpty()) {
                val args: MutableList<TranslationArgument?> = ArrayList(component.arguments())
                var i = 0

                val size = args.size
                while (i < size) {
                    val arg = args[i]
                    if (arg!!.value() is Component) {
                        args[i] = TranslationArgument.component(this.render((arg.value() as Component), context))
                    }
                    ++i
                }

                builder.arguments(args)
            }

            return this.mergeStyleAndOptionallyDeepRender(component, builder, context)
        }
    }

}