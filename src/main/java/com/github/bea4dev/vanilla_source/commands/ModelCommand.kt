package com.github.bea4dev.vanilla_source.commands

import com.github.bea4dev.vanilla_source.resource.model.EntityModelResources
import com.github.bea4dev.vanilla_source.server.entity.ModelViewer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.command.builder.suggestion.SuggestionEntry
import net.minestom.server.entity.Player
import net.minestom.server.timer.TaskSchedule
import net.worldseed.multipart.ModelLoader
import net.worldseed.multipart.animations.AnimationHandlerImpl
import java.util.function.Function

class ModelCommand : Command("model") {

    init {
        val modelNameArg = ArgumentType.String("model-name")
        modelNameArg.setSuggestionCallback { sender, _, suggestion ->
            if (sender !is Player) {
                return@setSuggestionCallback
            }

            for (model in EntityModelResources.models()) {
                suggestion.addEntry(SuggestionEntry(model))
            }
        }
        modelNameArg.defaultValue = Function { _ -> "" }

        val scaleArg = ArgumentType.Float("scale")
        scaleArg.defaultValue = Function { _ -> 1.0F }

        val animationNameArg = ArgumentType.String("animation-name")
        animationNameArg.setSuggestionCallback { sender, _, suggestion ->
            if (sender !is Player) {
                return@setSuggestionCallback
            }

            if (modelNameArg.id !in EntityModelResources.models()) {
                return@setSuggestionCallback
            }

            val animations = ModelLoader.loadAnimations("${modelNameArg.id}.bbmodel").get("animations").asJsonObject.keySet()

            for (animation in animations) {
                suggestion.addEntry(SuggestionEntry(animation))
            }
        }
        animationNameArg.defaultValue = Function { _ -> "" }

        addSyntax({ sender, context ->
            if (sender !is Player) {
                sender.sendMessage(Component.text("Player only.").color(NamedTextColor.RED))
                return@addSyntax
            }

            val modelName = context[modelNameArg]
            val scale = context[scaleArg]
            val animationName = context[animationNameArg]

            if (modelName !in EntityModelResources.models()) {
                sender.sendMessage(Component.text("Model '${modelName}' is not found!").color(NamedTextColor.RED))
                return@addSyntax
            }

            val entity = ModelViewer("$modelName.bbmodel")
            entity.init(sender.instance, sender.position, scale)
            entity.addViewer(sender)

            val animationHandler = AnimationHandlerImpl(entity)
            if (animationName != "") {
                animationHandler.playRepeat(animationName)
            }

            MinecraftServer.getSchedulerManager().scheduleTask({
                entity.destroy()
                animationHandler.destroy()
            }, TaskSchedule.tick(1200), TaskSchedule.stop())

            sender.sendMessage(Component.text("Spawn!").color(NamedTextColor.GREEN))
        }, modelNameArg, scaleArg, animationNameArg)
    }

}