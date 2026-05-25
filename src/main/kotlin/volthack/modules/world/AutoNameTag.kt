package volthack.modules.world

import net.minecraft.client.Minecraft
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Items
import volthack.event.EventBus
import volthack.event.TickEvent
import volthack.setting.Category
import volthack.setting.Module

object AutoNameTag : Module("AutoNameTag", "Automatically applies Name Tags to nearby living entities that do not already have a custom name", Category.WORLD) {
    private val reachRange by float("Range", 4.5f, 2.0f, 6.0f, 0.1f)
    private val useDelay by int("Delay Ticks", 10, 2, 40)

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

        // 1. Locate Name Tag in hotbar
        val tagSlot = (0..8).firstOrNull { player.inventory.getItem(it).item == Items.NAME_TAG } ?: return

        // 2. Scan for nearby living entities without a custom name
        val target = world.entitiesForRendering()
            .filterIsInstance<LivingEntity>()
            .filter { it != player && it.isAlive }
            .filter { player.distanceToSqr(it) <= (reachRange * reachRange).toDouble() }
            .firstOrNull { !it.hasCustomName() }

        if (target != null) {
            // Swap to Name Tag slot
            val prevSlot = player.inventory.selectedSlot
            player.inventory.selectedSlot = tagSlot

            // Interact with entity
            mc.gameMode?.interact(player, target, InteractionHand.MAIN_HAND)
            player.swing(InteractionHand.MAIN_HAND)

            // Restore slot
            player.inventory.selectedSlot = prevSlot

            cooldown = useDelay
        }
    }

    override fun onDisable() {
        cooldown = 0
    }
}
