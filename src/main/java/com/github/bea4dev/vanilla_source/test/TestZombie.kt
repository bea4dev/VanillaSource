package com.github.bea4dev.vanilla_source.test

import com.github.bea4dev.vanilla_source.resource.model.EntityModelResource
import com.github.bea4dev.vanilla_source.server.entity.EnemyModelEntity
import net.minestom.server.entity.EntityType
import team.unnamed.hephaestus.minestom.MinestomModelEngine

class TestZombie : EnemyModelEntity(
    EntityType.ZOMBIE,
    EntityModelResource.getInstance()["normal_zombie_be"]!!,
    MinestomModelEngine.BoneType.AREA_EFFECT_CLOUD
) {

}