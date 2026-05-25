package volthack.modules.render

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.SpawnerBlockEntity
import org.joml.Matrix4f
import volthack.event.EventBus
import volthack.event.Render3DEvent
import volthack.gui.font.GUIFontRenderer
import volthack.setting.Category
import volthack.setting.Module
import volthack.util.render.Render2DUtil
import volthack.util.render.Render3DUtil

object Spawner : Module("Spawner", "Highlights spawners and shows information about them", Category.RENDER) {
    val range by float("Range", 24f, 5f, 64f, 1f, "Scan range for spawners")
    val color by color("Color", 0xFFFF9F43.toInt(), "Spawner outline color")

    private data class SpawnerTag(
        val label: String,
        val distance: Float,
        val sx: Double,
        val sy: Double
    )

    private var tags = listOf<SpawnerTag>()
    private var modelView = Matrix4f()
    private var projection = Matrix4f()
    private var hasData = false

    init {
        EventBus.listen<Render3DEvent> { onRender3D(it) }
    }

    private fun onRender3D(event: Render3DEvent) {
        if (!enabled) return
        modelView = event.modelViewMatrix
        projection = event.projectionMatrix
        hasData = true

        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val level = mc.level ?: return
        val cam = mc.gameRenderer.mainCamera.position()

        val newTags = mutableListOf<SpawnerTag>()
        val pChunkX = player.blockPosition().x shr 4
        val pChunkZ = player.blockPosition().z shr 4
        val chunkR = (range.toInt() shr 4) + 1

        val col = color
        val r = (col shr 16) and 0xFF
        val g = (col shr 8) and 0xFF
        val b = col and 0xFF

        for (cx in (pChunkX - chunkR)..(pChunkX + chunkR)) {
            for (cz in (pChunkZ - chunkR)..(pChunkZ + chunkR)) {
                val chunk = level.getChunk(cx, cz) ?: continue
                for ((pos, be) in chunk.blockEntities) {
                    if (be is SpawnerBlockEntity) {
                        val dSqr = player.distanceToSqr(pos.x.toDouble() + 0.5, pos.y.toDouble() + 0.5, pos.z.toDouble() + 0.5)
                        if (dSqr <= range * range) {
                            // Render 3D block outline
                            Render3DUtil.drawBlockOutline(
                                modelView,
                                pos.x.toDouble() + 0.5,
                                pos.y.toDouble(),
                                pos.z.toDouble() + 0.5,
                                1.0f, 1.0f,
                                r, g, b, 255,
                                true,
                                2.0f
                            )

                            // Read mob type
                            val mobName = try {
                                val field = net.minecraft.world.level.BaseSpawner::class.java.getDeclaredField("nextSpawnData")
                                field.isAccessible = true
                                val data = field.get(be.spawner) as? net.minecraft.world.level.SpawnData
                                val id = data?.entityToSpawn?.getString("id")?.toString() ?: "Unknown"
                                val clean = if (id.contains(":")) id.substringAfter(":") else id
                                clean.replace("_", " ").replaceFirstChar { it.uppercase() }
                            } catch (e: Exception) {
                                "Mob"
                            }

                            val dist = Math.sqrt(dSqr).toFloat()
                            val projected = Render2DUtil.projectToScreen(
                                pos.x + 0.5,
                                pos.y + 1.2,
                                pos.z + 0.5,
                                modelView,
                                projection
                            )
                            if (projected != null) {
                                newTags.add(SpawnerTag(mobName, dist, projected.x, projected.y))
                            }
                        }
                    }
                }
            }
        }
        tags = newTags
    }

    fun render2D(ctx: GuiGraphics) {
        if (!enabled || !hasData) return
        val mc = Minecraft.getInstance()

        for (tag in tags) {
            val scaleVal = 1.0f
            val pose = (ctx as volthack.mixin.render.GuiGraphicsAccessor).pose
            pose.pushMatrix()
            pose.translate(tag.sx.toFloat(), tag.sy.toFloat())
            pose.scale(scaleVal, scaleVal)
            pose.translate(-tag.sx.toFloat(), -tag.sy.toFloat())

            val lineH = GUIFontRenderer.height + 2
            val text = "${tag.label} Spawner"
            val distText = "%.1fm".format(tag.distance)

            val w1 = GUIFontRenderer.width(text)
            val w2 = GUIFontRenderer.width(distText)
            val maxW = maxOf(w1, w2)
            val bgW = maxW + 10
            val bgH = lineH * 2 + 4

            ctx.fill(
                (tag.sx - bgW / 2.0).toInt(), (tag.sy - bgH / 2.0).toInt(),
                (tag.sx + bgW / 2.0).toInt(), (tag.sy + bgH / 2.0).toInt(),
                0x90000000.toInt()
            )

            Render2DUtil.drawText(ctx, text, tag.sx, tag.sy - bgH / 2.0 + 2, 0xFFFFCC00.toInt(), centerX = true)
            Render2DUtil.drawText(ctx, distText, tag.sx, tag.sy - bgH / 2.0 + 2 + lineH, 0xFFAAAAAA.toInt(), centerX = true)

            pose.popMatrix()
        }
    }
}
