package volthack.modules.render

import net.minecraft.client.Minecraft
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.animal.Animal
import volthack.event.EventBus
import volthack.event.Render3DEvent
import volthack.setting.Category
import volthack.setting.Module
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.world.phys.Vec3

object Tracers : Module("Tracers", "Draws lines pointing to nearby entities", Category.RENDER) {
    val players by boolean("Players", true, "Trace lines to other players")
    val monsters by boolean("Monsters", false, "Trace lines to hostile mobs")
    val passives by boolean("Passives", false, "Trace lines to passive animals")
    val color by color("Color", 0xFF6C63FF.toInt(), "The color of tracer lines")
    val range by float("Range", 64.0f, 8.0f, 128.0f, 4.0f, "Maximum trace distance")

    init {
        EventBus.listen<Render3DEvent> { onRender3D(it) }
    }

    private fun transform(vec: Vec3, matrix: org.joml.Matrix4f): org.joml.Vector3f {
        val mc = Minecraft.getInstance()
        val cam = mc.gameRenderer.mainCamera.position()
        val dest = org.joml.Vector3f()
        matrix.transformPosition(
            (vec.x - cam.x).toFloat(),
            (vec.y - cam.y).toFloat(),
            (vec.z - cam.z).toFloat(),
            dest
        )
        return dest
    }

    private fun onRender3D(event: Render3DEvent) {
        if (!enabled) return

        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val world = mc.level ?: return

        val entities = world.entitiesForRendering()
            .filterIsInstance<LivingEntity>()
            .filter { it != player && it.isAlive }
            .filter { player.distanceToSqr(it) <= range * range }
            .filter { isValidTarget(it) }

        if (entities.isEmpty()) return

        val bufferSource = mc.renderBuffers().bufferSource()
        val consumer = bufferSource.getBuffer(RenderTypes.lines())
        val pose = PoseStack().last()

        val col = color
        val a = 255
        val r = (col shr 16) and 0xFF
        val g = (col shr 8) and 0xFF
        val b = col and 0xFF
        val argbColor = (a shl 24) or (r shl 16) or (g shl 8) or b

        // Start point is exactly 0.1 blocks ahead of the camera in camera-space (the crosshair)
        val startX = 0.0f
        val startY = 0.0f
        val startZ = -0.1f

        for (entity in entities) {
            val endWorld = Vec3(
                entity.xo + (entity.x - entity.xo) * event.partialTicks,
                entity.yo + (entity.y - entity.yo) * event.partialTicks + (entity.bbHeight / 2.0),
                entity.zo + (entity.z - entity.zo) * event.partialTicks
            )

            val endCam = transform(endWorld, event.modelViewMatrix)

            consumer.addVertex(pose, startX, startY, startZ)
                .setColor(argbColor)
                .setNormal(pose, 0.0f, 1.0f, 0.0f)
                .setLineWidth(1.5f)

            consumer.addVertex(pose, endCam.x, endCam.y, endCam.z)
                .setColor(argbColor)
                .setNormal(pose, 0.0f, 1.0f, 0.0f)
                .setLineWidth(1.5f)
        }

        bufferSource.endBatch(RenderTypes.lines())
    }

    private fun isValidTarget(entity: LivingEntity): Boolean {
        if (entity is Player) return players
        if (entity is Monster) return monsters
        if (entity is Animal) return passives
        return false
    }
}
