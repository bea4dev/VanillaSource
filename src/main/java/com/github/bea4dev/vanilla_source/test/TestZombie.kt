package com.github.bea4dev.vanilla_source.test

import com.github.bea4dev.vanilla_source.resource.model.EntityModelResource
import com.github.bea4dev.vanilla_source.server.coroutine.tick
import com.github.bea4dev.vanilla_source.server.entity.EnemyModelEntity
import kotlinx.coroutines.delay
import net.minestom.server.entity.EntityType
import team.unnamed.hephaestus.minestom.MinestomModelEngine
import kotlin.random.Random

class TestZombie : EnemyModelEntity(EntityType.ZOMBIE, EntityModelResource.getInstance()["normal_zombie_be"]!!, MinestomModelEngine.BoneType.AREA_EFFECT_CLOUD) {
    override val attackActions = EnemyActionProvider(
        attackAction = {
            when (Random.nextInt(3)) {
                0 -> {
                    playAnimation("attack")
                    delay(18.tick)




                }

                1 -> {}
                else -> {}
            }
        },
        {},
        {}
    )



}