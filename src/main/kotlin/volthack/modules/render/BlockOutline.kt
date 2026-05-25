package volthack.modules.render

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import volthack.event.EventBus
import volthack.event.Render3DEvent
import volthack.setting.Category
import volthack.setting.Module
import volthack.util.render.Render3DUtil

object BlockOutline : Module("BlockOutline", "Highlights the block you are looking at", Category.RENDER) {
    private val mode by mode("Mode", listOf("Outline", "Full", "Both"), "Both")
    private val colorMode by mode("Color Mode", listOf("Static", "Rainbow"), "Static")
    private val customColor by color("Color", 0xFF6C63FF.toInt())
    private val lineWidth by float("Line Width", 2.0f, 0.5f, 5.0f, 0.5f)
    private val throughWalls by boolean("Through Walls", false)

    init {
        EventBus.listen<Render3DEvent> { onRender3D(it) }
    }

    fun getCurrentColor(): Int {
        val time = System.currentTimeMillis()
        return if (colorMode == "Rainbow") {
            val hue = (time * 0.001f) % 1.0f
            java.awt.Color.HSBtoRGB(hue, 0.8f, 1.0f)
        } else {
            customColor
        }
    }

    fun getOutlineMode(): String {
        return mode
    }

    private fun onRender3D(event: Render3DEvent) {
        if (!enabled) return

        val mc = Minecraft.getInstance()
        val hit = mc.hitResult
        if (hit == null || hit.type != HitResult.Type.BLOCK) return

        val blockHit = hit as BlockHitResult
        val pos = blockHit.blockPos

        val camera = mc.gameRenderer.mainCamera
        val camPos = camera.position()

        val worldX = pos.x.toDouble() + 0.5
        val worldY = pos.y.toDouble()
        val worldZ = pos.z.toDouble() + 0.5

        val baseColor = getCurrentColor()

        val a = (baseColor shr 24 and 255).let { if (it == 0) 255 else it }
        val r = (baseColor shr 16 and 255)
        val g = (baseColor shr 8 and 255)
        val b = (baseColor and 255)

        if ((mode == "Outline" || mode == "Both") && throughWalls) {
            Render3DUtil.drawBlockOutline(
                event.modelViewMatrix,
                worldX, worldY, worldZ,
                1.0f, 1.0f,
                r, g, b, a,
                throughWalls,
                lineWidth
            )
        }

        if (mode == "Full" || mode == "Both") {
            Render3DUtil.drawBand(
                event.modelViewMatrix,
                worldX, worldY, worldZ,
                0.4f, 1.0f,
                r, g, b, (a * 0.25f).toInt(),
                0.0,
                throughWalls
            )
        }
    }
}

