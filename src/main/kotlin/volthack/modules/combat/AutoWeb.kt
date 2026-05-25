package volthack.modules.combat

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import volthack.event.EventBus
import volthack.event.TickEvent
import volthack.setting.Category
import volthack.setting.Module
import volthack.util.combat.CombatUtils
import volthack.util.player.HotbarUtils
import kotlin.math.floor

object AutoWeb : Module("AutoWeb", "Automatically places cobwebs at the target's feet or head", Category.COMBAT) {
    private val range by float("Range", 4.5f, 2.0f, 6.0f, 0.1f, "Target scan range")
    private val feet by boolean("Feet", true, "Place cobweb at target's feet")
    private val head by boolean("Head", false, "Place cobweb at target's head")
    private val delay by int("Delay (Ticks)", 2, 0, 20, "Tick delay between placements")

    private var tickTimer = 0

    init {
        EventBus.listen<TickEvent> { onTick() }
    }

    override fun onEnable() {
        tickTimer = 0
    }

    private fun onTick() {
        if (!enabled) return
        val mc = Minecraft.getInstance()
        if (mc.player == null || mc.level == null) return

        tickTimer++
        if (tickTimer < delay) return

        val webSlot = HotbarUtils.find { it.item == Blocks.COBWEB.asItem() }
        if (webSlot == -1) return

        val target = CombatUtils.getClosest(range.toDouble()) ?: return

        val feetPos = BlockPos(
            floor(target.x).toInt(),
            floor(target.y).toInt(),
            floor(target.z).toInt()
        )

        var placed = false

        if (feet && placeWebAt(feetPos, webSlot)) {
            placed = true
        }

        if (head && !placed && placeWebAt(feetPos.above(), webSlot)) {
            placed = true
        }

        if (placed) {
            tickTimer = 0
        }
    }

    private fun placeWebAt(pos: BlockPos, webSlot: Int): Boolean {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return false
        val level = mc.level ?: return false

        if (!level.getBlockState(pos).isAir) return false

        for (dir in Direction.entries) {
            val neighbor = pos.relative(dir)
            val state = level.getBlockState(neighbor)
            if (!state.isAir && state.fluidState.isEmpty) {
                val oldSlot = HotbarUtils.selectedSlot
                val hitResult = BlockHitResult(
                    Vec3.atCenterOf(neighbor).add(
                        Vec3(
                            dir.opposite.stepX * 0.5,
                            dir.opposite.stepY * 0.5,
                            dir.opposite.stepZ * 0.5
                        )
                    ),
                    dir.opposite,
                    neighbor,
                    false
                )

                HotbarUtils.select(webSlot)
                mc.gameMode?.useItemOn(player, InteractionHand.MAIN_HAND, hitResult)
                player.swing(InteractionHand.MAIN_HAND)
                HotbarUtils.select(oldSlot)
                return true
            }
        }
        return false
    }
}
