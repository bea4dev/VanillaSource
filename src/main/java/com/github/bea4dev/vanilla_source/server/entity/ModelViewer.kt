package com.github.bea4dev.vanilla_source.server.entity

import net.worldseed.multipart.GenericModelImpl

class ModelViewer(private val modelId: String): GenericModelImpl() {
    override fun getId(): String {
        return modelId
    }
}