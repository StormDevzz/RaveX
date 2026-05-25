package volthack.modules.player

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.BlockItem
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import volthack.event.EventBus
import volthack.event.Render3DEvent
import volthack.event.TickEvent
import volthack.setting.Category
import volthack.setting.Module
import volthack.util.render.Render3DUtil

object AirPlace : Module("AirPlace", "Allows placing blocks in the empty air and renders a preview box of where the block will go", Category.PLAYER) {
    private val range by float("Reach Range", 4.5f, 2.0f, 6.0f, 0.1f)
    private val previewColor by color("Color", 0xFF00FF88.toInt())

    private var targetPos: BlockPos? = null
    private var lastPlaceTime = 0L

    init {
        EventBus.listen<TickEvent> { onTick() }
        EventBus.listen<Render3DEvent> { onRender3D(it) }
    }

    private fun onTick() {
        if (!enabled) {
            targetPos = null
            return
        }

        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val world = mc.level ?: return

        // Check if holding a placeable block in either hand
        val hand = when {
            player.mainHandItem.item is BlockItem -> InteractionHand.MAIN_HAND
            player.offhandItem.item is BlockItem -> InteractionHand.OFF_HAND
            else -> {
                targetPos = null
                return
            }
        }

        // Trace looking vector
        val eyePos = player.getEyePosition(1.0f)
        val lookVec = player.getViewVector(1.0f)
        val targetX = eyePos.x + lookVec.x * range
        val targetY = eyePos.y + lookVec.y * range
        val targetZ = eyePos.z + lookVec.z * range
        val pos = BlockPos(targetX.toInt(), targetY.toInt(), targetZ.toInt())

        // Check if the slot can be placed (must be air/replaceable)
        val state = world.getBlockState(pos)
        if (state.isAir || state.canBeReplaced()) {
            targetPos = pos

            // Place when right-clicking
            if (mc.options.keyUse.isDown && System.currentTimeMillis() - lastPlaceTime > 150) {
                lastPlaceTime = System.currentTimeMillis()

                val hitResult = BlockHitResult(
                    Vec3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5),
                    Direction.UP,
                    pos,
                    false
                )
                mc.gameMode?.useItemOn(player, hand, hitResult)
                player.swing(hand)
            }
        } else {
            targetPos = null
        }
    }

    private fun onRender3D(event: Render3DEvent) {
        if (!enabled) return
        val pos = targetPos ?: return

        val col = previewColor
        val r = (col shr 16) and 0xFF
        val g = (col shr 8) and 0xFF
        val b = col and 0xFF
        val a = (col shr 24) and 0xFF

        Render3DUtil.drawBlockOutline(
            event.modelViewMatrix,
            pos.x.toDouble() + 0.5,
            pos.y.toDouble(),
            pos.z.toDouble() + 0.5,
            1.0f, 1.0f,
            r, g, b, if (a == 0) 255 else a,
            true,
            2.0f
        )
    }
}
