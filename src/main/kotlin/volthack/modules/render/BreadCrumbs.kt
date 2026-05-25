package volthack.modules.render

import net.minecraft.client.Minecraft
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3
import volthack.event.EventBus
import volthack.event.Render3DEvent
import volthack.setting.Category
import volthack.setting.Module
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.rendertype.RenderTypes
import java.util.concurrent.ConcurrentHashMap

object BreadCrumbs : Module("BreadCrumbs", "Draws paths showing your and other entities' movement trail", Category.RENDER) {
    val onlySelf by boolean("Only Self", true, "Only trace your own movements")
    val color by color("Color", 0xFFFF007F.toInt(), "Path trail color")
    val maxPoints by int("Max Points", 200, 50, 1000, "Max trail points per entity")

    private val trails = ConcurrentHashMap<Int, MutableList<Vec3>>()

    init {
        EventBus.listen<Render3DEvent> { onRender3D(it) }
    }

    private fun transform(vec: Vec3, matrix: org.joml.Matrix4f): org.joml.Vector3f {
        val dest = org.joml.Vector3f()
        matrix.transformPosition(vec.x.toFloat(), vec.y.toFloat(), vec.z.toFloat(), dest)
        return dest
    }

    private fun onRender3D(event: Render3DEvent) {
        if (!enabled) return

        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val world = mc.level ?: return

        // 1. Update trail data
        val entitiesToTrack = if (onlySelf) listOf(player) else world.entitiesForRendering()
            .filterIsInstance<LivingEntity>()
            .filter { it.isAlive }

        val activeIds = entitiesToTrack.map { it.id }.toSet()

        // Clean up trails of untracked/dead entities
        val iterator = trails.keys().iterator()
        while (iterator.hasNext()) {
            val id = iterator.next()
            if (id != player.id && !activeIds.contains(id)) {
                trails.remove(id)
            }
        }

        for (entity in entitiesToTrack) {
            val list = trails.computeIfAbsent(entity.id) { mutableListOf() }
            val currentPos = Vec3(
                entity.xo + (entity.x - entity.xo) * event.partialTicks,
                entity.yo + (entity.y - entity.yo) * event.partialTicks,
                entity.zo + (entity.z - entity.zo) * event.partialTicks
            )

            if (list.isEmpty() || list.last().distanceToSqr(currentPos) > 0.04) {
                list.add(currentPos)
                if (list.size > maxPoints) {
                    list.removeAt(0)
                }
            }
        }

        // 2. Render trails
        if (trails.isEmpty()) return

        val bufferSource = mc.renderBuffers().bufferSource()
        val consumer = bufferSource.getBuffer(RenderTypes.lines())
        val pose = PoseStack().last()

        val col = color
        val a = 255
        val r = (col shr 16) and 0xFF
        val g = (col shr 8) and 0xFF
        val b = col and 0xFF
        val argbColor = (a shl 24) or (r shl 16) or (g shl 8) or b

        for ((_, path) in trails) {
            if (path.size < 2) continue
            for (i in 0 until path.size - 1) {
                val p1 = path[i]
                val p2 = path[i + 1]

                val c1 = transform(p1, event.modelViewMatrix)
                val c2 = transform(p2, event.modelViewMatrix)

                consumer.addVertex(pose, c1.x, c1.y, c1.z)
                    .setColor(argbColor)
                    .setNormal(pose, 0.0f, 1.0f, 0.0f)
                    .setLineWidth(2.0f)

                consumer.addVertex(pose, c2.x, c2.y, c2.z)
                    .setColor(argbColor)
                    .setNormal(pose, 0.0f, 1.0f, 0.0f)
                    .setLineWidth(2.0f)
            }
        }

        bufferSource.endBatch(RenderTypes.lines())
    }

    override fun onDisable() {
        trails.clear()
    }
}
