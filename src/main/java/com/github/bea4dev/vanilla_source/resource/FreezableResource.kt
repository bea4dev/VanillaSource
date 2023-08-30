package com.github.bea4dev.vanilla_source.resource

abstract class FreezableResource {
    private var isFrozen = false

    protected fun assert() {
        if (isFrozen) {
            throw IllegalStateException("This resource has already been frozen!")
        }
    }

    protected fun freeze() { isFrozen = true }
}