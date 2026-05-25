package volthack.modules.render

import net.minecraft.client.Minecraft
import net.minecraft.world.item.*
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import volthack.event.EventBus
import volthack.event.Render3DEvent
import volthack.setting.Category
import volthack.setting.Module
import volthack.util.render.Render3DUtil
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.rendertype.RenderTypes

object Trajectories : Module("Trajectories", "Predicts and renders the flight path of your projectiles", Category.RENDER) {
    val color by color("Color", 0xFF00FFCC.toInt(), "Path rendering color")

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
        val level = mc.level ?: return
        val handStack = player.mainHandItem
        val offhandStack = player.offhandItem

        val stack = if (isProjectile(handStack)) handStack else if (isProjectile(offhandStack)) offhandStack else return
        val item = stack.item

        // Get initial velocity and gravity
        var vel = 1.5f
        var gravity = 0.03f
        var drag = 0.99f

        if (item is BowItem) {
            val charge = player.useItemRemainingTicks
            var useTicks = if (charge > 0) 72000 - charge else 0
            if (useTicks <= 0) {
                useTicks = 20 // Default to full charge for predicting path before releasing/charging
            }
            var power = useTicks.toFloat() / 20.0f
            power = (power * power + power * 2.0f) / 3.0f
            if (power > 1.0f) power = 1.0f
            vel = power * 3.0f
            gravity = 0.05f
        } else if (item is CrossbowItem) {
            vel = 3.15f
            gravity = 0.05f
        } else if (item is SnowballItem || item is EggItem || item is EnderpearlItem) {
            vel = 1.5f
            gravity = 0.03f
        } else if (item is ThrowablePotionItem) {
            vel = 0.5f
            gravity = 0.05f
        } else {
            vel = 1.5f
            gravity = 0.03f
        }

        var posX = player.xo + (player.x - player.xo) * event.partialTicks
        var posY = player.yo + (player.y - player.yo) * event.partialTicks + player.eyeHeight
        var posZ = player.zo + (player.z - player.zo) * event.partialTicks

        val yaw = player.yRot
        val pitch = player.xRot

        var motionX = -Math.sin(yaw * Math.PI / 180.0) * Math.cos(pitch * Math.PI / 180.0) * vel
        var motionY = -Math.sin(pitch * Math.PI / 180.0) * vel
        var motionZ = Math.cos(yaw * Math.PI / 180.0) * Math.cos(pitch * Math.PI / 180.0) * vel

        val path = mutableListOf<Vec3>()
        path.add(Vec3(posX, posY, posZ))

        var hitBlock: net.minecraft.world.phys.BlockHitResult? = null

        for (step in 0 until 150) {
            val p1 = Vec3(posX, posY, posZ)
            val p2 = Vec3(posX + motionX, posY + motionY, posZ + motionZ)

            val clipResult = level.clip(
                net.minecraft.world.level.ClipContext(
                    p1, p2,
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE,
                    player
                )
            )

            if (clipResult.type == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                path.add(clipResult.location)
                hitBlock = clipResult
                break
            }

            posX += motionX
            posY += motionY
            posZ += motionZ

            motionX *= drag
            motionY *= drag
            motionY -= gravity
            motionZ *= drag

            path.add(Vec3(posX, posY, posZ))
        }

        // Draw the path using lines
        val bufferSource = mc.renderBuffers().bufferSource()
        val consumer = bufferSource.getBuffer(RenderTypes.lines())
        val pose = PoseStack().last()

        val col = color
        val a = 255
        val r = (col shr 16) and 0xFF
        val g = (col shr 8) and 0xFF
        val b = col and 0xFF
        val argbColor = (a shl 24) or (r shl 16) or (g shl 8) or b

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

        bufferSource.endBatch(RenderTypes.lines())

        if (hitBlock != null) {
            val blockPos = hitBlock.blockPos
            Render3DUtil.drawBlockOutline(
                event.modelViewMatrix,
                blockPos.x.toDouble() + 0.5,
                blockPos.y.toDouble(),
                blockPos.z.toDouble() + 0.5,
                1.02f, 1.02f,
                r, g, b, 255,
                true,
                2.0f
            )
        }
    }

    private fun isProjectile(stack: ItemStack): Boolean {
        val item = stack.item
        return item is BowItem || item is CrossbowItem || item is EnderpearlItem || item is SnowballItem || item is EggItem || item is ThrowablePotionItem
    }
}
