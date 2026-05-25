package volthack.util.world.highway

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3

object HighwayUtils {
    private val mc get() = Minecraft.getInstance()
    private val player get() = mc.player
    private val world get() = mc.level

    // 1. Check if stack is obsidian
    fun isObsidian(stack: ItemStack): Boolean {
        return !stack.isEmpty && stack.item == Items.OBSIDIAN
    }

    // 2. Find obsidian slot in hotbar (0..8)
    fun findObsidianInHotbar(): Int {
        val p = player ?: return -1
        for (i in 0..8) {
            if (isObsidian(p.inventory.getItem(i))) {
                return i
            }
        }
        return -1
    }

    // 3. Calculate blocks for the highway platform
    fun calculateHighwayPlatform(center: BlockPos, dir: Direction, width: Int): List<BlockPos> {
        val list = mutableListOf<BlockPos>()
        val right = dir.clockWise
        val halfWidth = width / 2
        for (w in -halfWidth..halfWidth) {
            list.add(center.relative(right, w))
        }
        return list
    }

    // 4. Calculate blocks for highway side walls
    fun calculateHighwayWalls(center: BlockPos, dir: Direction, width: Int, wallHeight: Int): List<BlockPos> {
        val list = mutableListOf<BlockPos>()
        val right = dir.clockWise
        val halfWidth = width / 2
        val leftWallPos = center.relative(right, -halfWidth - 1)
        val rightWallPos = center.relative(right, halfWidth + 1)
        
        for (h in 1..wallHeight) {
            list.add(leftWallPos.above(h))
            list.add(rightWallPos.above(h))
        }
        return list
    }

    // 5. Check if a block position is replaceable/placeable
    fun isReplaceable(pos: BlockPos): Boolean {
        val w = world ?: return false
        val state = w.getBlockState(pos)
        return state.isAir || state.canBeReplaced()
    }

    // 6. Place obsidian block at target pos
    fun placeObsidian(pos: BlockPos, obsidianSlot: Int): Boolean {
        val p = player ?: return false
        val mc = Minecraft.getInstance()
        
        // Select slot
        val prevSlot = p.inventory.selectedSlot
        p.inventory.selectedSlot = obsidianSlot

        val vec3 = Vec3.atCenterOf(pos)
        val hitResult = BlockHitResult(vec3, Direction.UP, pos, false)
        
        mc.gameMode?.useItemOn(p, InteractionHand.MAIN_HAND, hitResult)
        p.swing(InteractionHand.MAIN_HAND)

        // Restore slot
        p.inventory.selectedSlot = prevSlot
        return true
    }

    // 7. Legitimately start mining/clearing an obstacle block in front
    fun breakObstacle(pos: BlockPos): Boolean {
        val w = world ?: return false
        val state = w.getBlockState(pos)
        if (state.isAir || state.liquid()) return false

        mc.gameMode?.startDestroyBlock(pos, Direction.UP)
        player?.swing(InteractionHand.MAIN_HAND)
        return true
    }

    // 8. Determine highway placement direction based on yaw
    fun getHighwayDirection(yaw: Float): Direction {
        return Direction.fromYRot(yaw.toDouble())
    }
}
