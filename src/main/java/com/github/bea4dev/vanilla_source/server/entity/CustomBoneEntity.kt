package com.github.bea4dev.vanilla_source.server.entity

import net.kyori.adventure.text.Component
import net.minestom.server.color.Color
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityCreature
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.display.ItemDisplayMeta
import net.minestom.server.entity.metadata.other.AreaEffectCloudMeta
import net.minestom.server.entity.metadata.other.ArmorStandMeta
import net.minestom.server.instance.Instance
import net.minestom.server.item.ItemMeta
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.metadata.LeatherArmorMeta
import net.minestom.server.network.packet.server.play.SetPassengersPacket
import team.unnamed.creative.base.Vector3Float
import team.unnamed.hephaestus.Bone
import team.unnamed.hephaestus.Minecraft
import team.unnamed.hephaestus.minestom.GenericBoneEntity
import team.unnamed.hephaestus.minestom.ModelEntity
import java.util.concurrent.CompletableFuture


@Suppress("UnstableApiUsage")
class CustomBoneEntity(
    view: ModelEntity,
    bone: Bone
) : GenericBoneEntity(EntityType.ITEM_DISPLAY) {
    private val view: ModelEntity
    private val bone: Bone
    private val armorstand: EntityCreature?

    // cached height offset, either SMALL_OFFSET
    // or LARGE_OFFSET
    private val offset: Float

    init {
        this.view = view
        this.bone = bone
        offset =
            if (bone.small()) Minecraft.ARMOR_STAND_SMALL_VERTICAL_OFFSET else Minecraft.ARMOR_STAND_DEFAULT_VERTICAL_OFFSET
        armorstand = EntityCreature(EntityType.ARMOR_STAND)
        initialize()
    }

    private fun initialize() {
        setNoGravity(true)
        isInvulnerable = true
        isSilent = true
        isInvulnerable = true
        //val cloudMeta = getEntityMeta() as AreaEffectCloudMeta
        //cloudMeta.radius = 0f
        val displayMeta = getEntityMeta() as ItemDisplayMeta
        displayMeta.itemStack = ItemStack.of(Material.DIAMOND_PICKAXE)
        val meta = armorstand!!.entityMeta as ArmorStandMeta
        meta.isSilent = true
        meta.isHasNoGravity = true
        meta.isSmall = bone.small()
        meta.isInvisible = true

        // set helmet with custom model data from our bone
        armorstand.helmet =
            BASE_HELMET.withMeta { itemMeta: ItemMeta.Builder ->
                itemMeta.customModelData(
                    bone.customModelData()
                )
            }
        armorstand.isAutoViewable = false
        armorstand.setNoGravity(true)
    }

    /**
     * Returns the holder view
     *
     * @return The view for this bone entity
     */
    fun view(): ModelEntity {
        return view
    }

    override fun bone(): Bone {
        return bone
    }

    override fun customName(displayName: Component?) {
        super.setCustomName(displayName)
    }

    override fun customName(): Component? {
        return super.getCustomName()
    }

    override fun customNameVisible(visible: Boolean) {
        super.setCustomNameVisible(visible)
    }

    override fun customNameVisible(): Boolean {
        return super.isCustomNameVisible()
    }

    /**
     * Colorizes this bone entity using
     * the specified color
     *
     * @param color The new bone color
     */
    public override fun colorize(color: Color?) {
        armorstand!!.helmet = armorstand.helmet.withMeta(
            LeatherArmorMeta::class.java
        ) { meta: LeatherArmorMeta.Builder -> meta.color(color) }
    }

    override fun colorize(r: Int, g: Int, b: Int) {
        colorize(Color(r, g, b))
    }

    override fun colorize(rgb: Int) {
        colorize(Color(rgb))
    }

    override fun position(position: Vector3Float) {
        teleport(
            view.position.add(
                position.x().toDouble(),
                (position.y() - offset).toDouble(),
                position.z().toDouble()
            )
        )
    }

    override fun rotation(rotation: Vector3Float) {
        val meta = armorstand!!.entityMeta as ArmorStandMeta
        meta.headRotation = Vec(
            rotation.x().toDouble(),
            rotation.y().toDouble(),
            rotation.z().toDouble()
        )
    }

    override fun teleport(position: Pos, chunks: LongArray?): CompletableFuture<Void> {
        armorstand!!.teleport(position, chunks)
        return super.teleport(position.sub(0.0, offset.toDouble(), 0.0), chunks)
    }

    override fun setInstance(instance: Instance, pos: Pos): CompletableFuture<Void> {
        return super.setInstance(instance, pos.sub(0.0, offset.toDouble(), 0.0)).thenAccept { ignored: Void? ->
            armorstand!!.setInstance(
                instance,
                pos
            ).join()
        }
    }

    override fun updateNewViewer(player: Player) {
        super.updateNewViewer(player)
        /*scheduleNextTick { entity: Entity? ->
            player.sendPacket(
                SetPassengersPacket(
                    entityId,
                    listOf(armorstand!!.entityId)
                )
            )
        }*/
    }

    override fun setAutoViewable(autoViewable: Boolean) {
        super.setAutoViewable(autoViewable)
        if (armorstand != null) {
            armorstand.isAutoViewable = autoViewable
        }
    }

    override fun kill() {
        armorstand!!.kill()
        super.kill()
    }

    companion object {
        private val BASE_HELMET = ItemStack.builder(Material.LEATHER_HORSE_ARMOR)
            .meta(
                LeatherArmorMeta.Builder()
                    .color(Color(0xFFFFFF))
                    .build()
            )
            .build()
    }
}


/**
 * Represents a [Bone] holder entity,
 * it is an armor stand with a LEATHER_HORSE_ARMOR
 * item as helmet using a custom model data to
 * apply the bone model
 *
 * @since 1.0.0
 */
internal class ArmorStandBoneEntity(
    private val view: ModelEntity,
    private val bone: Bone
) : GenericBoneEntity(EntityType.ARMOR_STAND) {
    // cached height offset, either SMALL_OFFSET
    // or LARGE_OFFSET
    private val offset: Float

    init {
        offset =
            if (bone.small()) Minecraft.ARMOR_STAND_SMALL_VERTICAL_OFFSET else Minecraft.ARMOR_STAND_DEFAULT_VERTICAL_OFFSET
        initialize()
    }

    private fun initialize() {
        val meta = getEntityMeta() as ArmorStandMeta
        meta.isSilent = true
        meta.isHasNoGravity = true
        meta.isSmall = bone.small()
        meta.isInvisible = true

        // set helmet with custom model data from our bone
        helmet = BASE_HELMET.withMeta { itemMeta: ItemMeta.Builder ->
            itemMeta.customModelData(
                bone.customModelData()
            )
        }
    }

    /**
     * Returns the holder view
     *
     * @return The view for this bone entity
     */
    fun view(): ModelEntity {
        return view
    }

    override fun bone(): Bone {
        return bone
    }

    override fun customName(displayName: Component) {
        super.setCustomName(displayName)
    }

    override fun customName(): Component {
        return super.getCustomName()!!
    }

    override fun customNameVisible(visible: Boolean) {
        super.setCustomNameVisible(visible)
    }

    override fun customNameVisible(): Boolean {
        return super.isCustomNameVisible()
    }

    /**
     * Colorizes this bone entity using
     * the specified color
     *
     * @param color The new bone color
     */
    public override fun colorize(color: Color) {
        helmet = helmet.withMeta(
            LeatherArmorMeta::class.java
        ) { meta: LeatherArmorMeta.Builder -> meta.color(color) }
    }

    override fun colorize(r: Int, g: Int, b: Int) {
        colorize(Color(r, g, b))
    }

    override fun colorize(rgb: Int) {
        colorize(Color(rgb))
    }

    override fun position(position: Vector3Float) {
        teleport(
            view.position.add(
                position.x().toDouble(),
                (
                        position.y() - offset).toDouble(),
                position.z()
                    .toDouble()
            )
        )
    }

    override fun rotation(rotation: Vector3Float) {
        val meta = getEntityMeta() as ArmorStandMeta
        meta.headRotation = Vec(
            rotation.x().toDouble(),
            rotation.y().toDouble(),
            rotation.z()
                .toDouble()
        )
    }

    override fun teleport(position: Pos, chunks: LongArray?): CompletableFuture<Void> {
        return super.teleport(position.sub(0.0, offset.toDouble(), 0.0), chunks)
    }

    override fun setInstance(instance: Instance, pos: Pos): CompletableFuture<Void> {
        return super.setInstance(instance, pos.sub(0.0, offset.toDouble(), 0.0))
    }

    companion object {
        private val BASE_HELMET = ItemStack.builder(Material.LEATHER_HORSE_ARMOR)
            .meta(
                LeatherArmorMeta.Builder()
                    .color(Color(0xFFFFFF))
                    .build()
            )
            .build()
    }
}




