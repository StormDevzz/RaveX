package volthack.modules.world

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntity
import volthack.event.EventBus
import volthack.event.TickEvent
import volthack.setting.Category
import volthack.setting.Module
import volthack.util.chat.ChatUtils

object StashFinder : Module("StashFinder", "Scans for container clusters in loaded chunks to locate stashes", Category.WORLD) {
    private val chests by boolean("Chests", true, "Search for Chests and Trapped Chests")
    private val shulkers by boolean("Shulker Boxes", true, "Search for Shulker Boxes")
    private val others by boolean("Others", false, "Search for Barrels, Dispensers, Droppers, Hoppers")
    private val minCount by int("Min Count", 4, 1, 50, "Minimum number of containers in a chunk to trigger notification")
    private val range by int("Range (Chunks)", 8, 2, 32, "Search radius in chunks")

    private val reportedStashes = mutableSetOf<String>()
    private var tickCounter = 0

    init {
        EventBus.listen<TickEvent> { onTick() }
    }

    private fun onTick() {
        if (!enabled) return
        tickCounter++
        // Scan every 40 ticks (2 seconds) to keep performance top-tier
        if (tickCounter >= 40) {
            tickCounter = 0
            scan()
        }
    }

    private fun scan() {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val level = mc.level ?: return

        val playerChunkX = player.blockPosition().x shr 4
        val playerChunkZ = player.blockPosition().z shr 4

        val chunkMap = mutableMapOf<String, MutableList<BlockPos>>()

        val r = range
        for (cx in (playerChunkX - r)..(playerChunkX + r)) {
            for (cz in (playerChunkZ - r)..(playerChunkZ + r)) {
                val chunk = level.getChunk(cx, cz) ?: continue
                val key = "$cx,$cz"

                for (pos in chunk.blockEntities.keys) {
                    val be = chunk.blockEntities[pos] ?: continue
                    if (isTarget(be)) {
                        chunkMap.computeIfAbsent(key) { mutableListOf() }.add(pos)
                    }
                }
            }
        }

        for ((key, positions) in chunkMap) {
            if (positions.size >= minCount) {
                if (reportedStashes.add(key)) {
                    val first = positions.first()
                    ChatUtils.addChatMessage(
                        "§7[§6StashFinder§7] §d§lStash Alert! §fLocation: §aX: ${first.x}, Y: ${first.y}, Z: ${first.z} §7(${positions.size} containers in chunk $key)"
                    )
                    mc.level?.playSound(
                        player,
                        player.x, player.y, player.z,
                        net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(),
                        net.minecraft.sounds.SoundSource.AMBIENT,
                        1.0f,
                        1.0f
                    )
                }
            }
        }
    }

    private fun isTarget(be: BlockEntity): Boolean {
        val name = be::class.java.simpleName
        if (chests && name.contains("Chest", ignoreCase = true)) {
            return true
        }
        if (shulkers && name.contains("Shulker", ignoreCase = true)) {
            return true
        }
        if (others && (
            name.contains("Barrel", ignoreCase = true) ||
            name.contains("Dispenser", ignoreCase = true) ||
            name.contains("Dropper", ignoreCase = true) ||
            name.contains("Hopper", ignoreCase = true)
        )) {
            return true
        }
        return false
    }

    override fun onEnable() {
        reportedStashes.clear()
        tickCounter = 40 // Scan immediately upon enable
    }
}
