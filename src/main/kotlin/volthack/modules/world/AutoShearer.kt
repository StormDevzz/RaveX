package volthack.modules.world

import net.minecraft.client.Minecraft
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.Items
import volthack.event.EventBus
import volthack.event.TickEvent
import volthack.setting.Category
import volthack.setting.Module

object AutoShearer : Module("AutoShearer", "Automatically shears nearby sheep when you are holding shears or have them in your hotbar", Category.WORLD) {
    private val reachRange by float("Range", 4.5f, 2.0f, 6.0f, 0.1f)
    private val shearDelay by int("Shear Delay Ticks", 4, 1, 20)

    private var cooldown = 0

    init {
        EventBus.listen<TickEvent> { onTick() }
    }

    private fun onTick() {
        if (!enabled) return

        if (cooldown > 0) {
            cooldown--
            return
        }

        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val world = mc.level ?: return

        // 1. Locate shears in hotbar
        val shearsSlot = (0..8).firstOrNull { player.inventory.getItem(it).item == Items.SHEARS } ?: return

        // 2. Scan for nearby sheep that can be sheared (using descriptionId to bypass compile-time class imports)
        val sheep = world.entitiesForRendering()
            .filter { it.isAlive && it.type.descriptionId.contains("sheep", ignoreCase = true) }
            .filter { player.distanceToSqr(it) <= (reachRange * reachRange).toDouble() }
            .firstOrNull {
                try {
                    val isShearedMethod = it.javaClass.getMethod("isSheared")
                    !(isShearedMethod.invoke(it) as Boolean)
                } catch (_: Exception) {
                    false
                }
            }

        if (sheep != null) {
            // Swap to shears
            val prevSlot = player.inventory.selectedSlot
            player.inventory.selectedSlot = shearsSlot

            // Interact with the sheep
            mc.gameMode?.interact(player, sheep, InteractionHand.MAIN_HAND)
            player.swing(InteractionHand.MAIN_HAND)

            // Restore slot
            player.inventory.selectedSlot = prevSlot

            cooldown = shearDelay
        }
    }

    override fun onDisable() {
        cooldown = 0
    }
}
