package com.github.bea4dev.vanilla_source.commands

import com.github.bea4dev.vanilla_source.resource.model.EntityModelResource
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.command.builder.suggestion.SuggestionEntry
import net.minestom.server.entity.Player
import net.minestom.server.timer.TaskSchedule
import java.util.function.Function

class ModelCommand : Command("model") {

    init {
        val modelNameArg = ArgumentType.String("model-name")
        modelNameArg.setSuggestionCallback { sender, _, suggestion ->
            if (sender !is Player) {
                return@setSuggestionCallback
            }

            for (model in EntityModelResource.getInstance().getModels()) {
                suggestion.addEntry(SuggestionEntry(model.name()))
            }
        }
        modelNameArg.defaultValue = Function { _ -> "" }

        val animationNameArg = ArgumentType.String("animation-name")
        animationNameArg.setSuggestionCallback { sender, context, suggestion ->
            if (sender !is Player) {
                return@setSuggestionCallback
            }

            val resource = EntityModelResource.getInstance()
            val model = resource[context[modelNameArg.id]] ?: return@setSuggestionCallback

            for (animation in model.animations()) {
                suggestion.addEntry(SuggestionEntry(animation.key))
            }
        }
        animationNameArg.defaultValue = Function { _ -> "" }

        addSyntax({ sender, context ->
            if (sender !is Player) {
                sender.sendMessage(Component.text("Player only.").color(NamedTextColor.RED))
                return@addSyntax
            }

            val modelName = context[modelNameArg]
            val animationName = context[animationNameArg]
            val resource = EntityModelResource.getInstance()
            val model = resource[modelName]

            if (model == null) {
                sender.sendMessage(Component.text("Model '${modelName}' is not found!").color(NamedTextColor.RED))
                return@addSyntax
            }

            val view = resource.createModelEntityTracked(modelName, sender.instance, sender.position)
            if (animationName != "") {
                view.playAnimation(animationName)
            }

            MinecraftServer.getSchedulerManager().scheduleTask({ view.kill() }, TaskSchedule.tick(1200), TaskSchedule.stop())

            sender.sendMessage(Component.text("Spawn!").color(NamedTextColor.GREEN))
        }, modelNameArg, animationNameArg)
    }

}