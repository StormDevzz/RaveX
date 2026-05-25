package volthack.modules.world

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.inventory.ClickType
import volthack.event.EventBus
import volthack.event.TickEvent
import volthack.setting.Category
import volthack.setting.Module
import volthack.util.world.highway.HighwayUtils

object HighwayMake : Module("HighwayMake", "Builds automated Obsidian Highways (platforms and walls) and mines obstacles in front of you", Category.WORLD) {
    private val width by int("Width", 3, 1, 7)
    private val buildWalls by boolean("Build Walls", false, "Build safety walls on the side of the highway")
    private val wallHeight by int("Wall Height", 2, 1, 4)
    private val mineObstacles by boolean("Clear Obstacles", true, "Mine blocks in your way while building")
    private val autoWalk by boolean("Auto Walk", false, "Automatically walks forward while building the highway")

    private var placingTimer = 0

    init {
        EventBus.listen<TickEvent> { onTick() }
    }

    private fun onTick() {
        if (!enabled) return

        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val world = mc.level ?: return

        // 1. Locate Obsidian
        var obsSlot = HighwayUtils.findObsidianInHotbar()
        if (obsSlot == -1) {
            // Check inventory and swap to slot 8
            val inv = player.inventory
            val invSlot = (9..35).firstOrNull { HighwayUtils.isObsidian(inv.getItem(it)) }
            if (invSlot != null) {
                mc.gameMode?.handleInventoryMouseClick(player.containerMenu.containerId, invSlot, 8, ClickType.SWAP, player)
                obsSlot = 8
            }
        }

        if (obsSlot == -1) {
            // No obsidian found, stop walking and pause
            if (autoWalk) mc.options.keyUp.isDown = false
            return
        }

        val dir = HighwayUtils.getHighwayDirection(player.yRot)
        val center = player.blockPosition().below()

        // 2. Clear obstacles if configured
        if (mineObstacles) {
            val forwardPos = player.blockPosition().relative(dir)
            val obstacles = listOf(forwardPos, forwardPos.above())
            for (obs in obstacles) {
                if (world.getBlockState(obs).isSolid) {
                    HighwayUtils.breakObstacle(obs)
                    if (autoWalk) mc.options.keyUp.isDown = false
                    return // Prioritize mining over placing
                }
            }
        }

        // 3. Build Highway
        val platformBlocks = HighwayUtils.calculateHighwayPlatform(center, dir, width)
        val wallBlocks = if (buildWalls) HighwayUtils.calculateHighwayWalls(center, dir, width, wallHeight) else emptyList()
        val allBlocks = platformBlocks + wallBlocks

        var placedAny = false
        placingTimer++

        if (placingTimer >= 2) {
            placingTimer = 0
            for (pos in allBlocks) {
                if (HighwayUtils.isReplaceable(pos)) {
                    HighwayUtils.placeObsidian(pos, obsSlot)
                    placedAny = true
                    break // Place one block per 2 ticks to bypass block-placing speed limits/kicks
                }
            }
        }

        // 4. AutoWalk logic
        if (autoWalk) {
            // Keep walking forward as long as the immediate platform under player is placed
            val immediatePlatform = HighwayUtils.calculateHighwayPlatform(center, dir, width)
            val allPlaced = immediatePlatform.none { HighwayUtils.isReplaceable(it) }
            mc.options.keyUp.isDown = allPlaced
        }
    }

    override fun onDisable() {
        val mc = Minecraft.getInstance()
        if (autoWalk) {
            mc.options.keyUp.isDown = false
        }
    }
}
