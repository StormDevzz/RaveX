package volthack.modules.player

import net.minecraft.client.Minecraft
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.item.ItemStack
import volthack.event.EventBus
import volthack.event.TickEvent
import volthack.setting.Category
import volthack.setting.Module

object AutoFix : Module("AutoFix", "Automatically swaps damaged items to your hands to repair them when XP orbs are nearby", Category.PLAYER) {
    private val threshold by int("Threshold %", 20, 5, 90)
    private val scanRange by float("XP Range", 6f, 2f, 16f, 0.5f)
    private val repairSlot by mode("Repair Hand", listOf("Offhand", "Mainhand"), "Offhand")

    private var originalSlot = -1
    private var activeRepairSlot = -1
    private var isRepairing = false

    init {
        EventBus.listen<TickEvent> { onTick() }
    }

    private fun onTick() {
        if (!enabled) return

        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val world = mc.level ?: return

        // 1. Check if there are XP orbs nearby
        val xpOrbs = world.entitiesForRendering()
            .filterIsInstance<ExperienceOrb>()
            .any { player.distanceToSqr(it) <= scanRange * scanRange }

        if (!xpOrbs) {
            // No XP orbs nearby, swap repaired/original item back if we were repairing
            if (isRepairing) {
                restoreItem()
            }
            return
        }

        // 2. If we are currently repairing, check if the item is fully repaired
        if (isRepairing) {
            val repairingStack = if (repairSlot == "Offhand") player.offhandItem else player.mainHandItem
            if (repairingStack.isEmpty || repairingStack.damageValue == 0) {
                restoreItem()
            }
            return
        }

        // 3. Find a damaged item in inventory (slots 0..35 or armor slots 36..39)
        val inv = player.inventory
        var bestSlot = -1
        var lowestPct = 1.0f

        // Search slots:
        // Hotbar & Main inventory: 0..35
        // Armor: 36..39 (36 = FEET, 37 = LEGS, 38 = CHEST, 39 = HEAD)
        for (i in 0..39) {
            val stack = inv.getItem(i)
            if (stack.isEmpty || !stack.isDamageableItem) continue

            // Only repair if it has damage
            val damage = stack.damageValue
            if (damage <= 0) continue

            val pct = (stack.maxDamage - damage).toFloat() / stack.maxDamage.toFloat()
            if (pct < (threshold / 100f) && pct < lowestPct) {
                lowestPct = pct
                bestSlot = i
            }
        }

        // 4. Swap the damaged item to the hand to repair
        if (bestSlot != -1) {
            isRepairing = true
            originalSlot = bestSlot

            if (repairSlot == "Offhand") {
                // Swap to Offhand (slot 40 in inventory menu)
                val menuId = player.containerMenu.containerId
                val slotToClick = if (bestSlot < 9) bestSlot + 36 else bestSlot
                
                // Swap slot to offhand
                mc.gameMode?.handleInventoryMouseClick(menuId, slotToClick, 40, ClickType.SWAP, player)
                activeRepairSlot = 40
            } else {
                // Swap to selected hotbar slot
                val targetHotbar = inv.selectedSlot
                if (bestSlot in 0..8) {
                    inv.selectedSlot = bestSlot
                } else {
                    val menuId = player.containerMenu.containerId
                    mc.gameMode?.handleInventoryMouseClick(menuId, bestSlot, targetHotbar, ClickType.SWAP, player)
                }
                activeRepairSlot = targetHotbar
            }
        }
    }

    private fun restoreItem() {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        
        if (originalSlot != -1) {
            val menuId = player.containerMenu.containerId
            if (repairSlot == "Offhand" && activeRepairSlot == 40) {
                val slotToClick = if (originalSlot < 9) originalSlot + 36 else originalSlot
                mc.gameMode?.handleInventoryMouseClick(menuId, slotToClick, 40, ClickType.SWAP, player)
            } else if (repairSlot == "Mainhand" && originalSlot >= 9) {
                mc.gameMode?.handleInventoryMouseClick(menuId, originalSlot, activeRepairSlot, ClickType.SWAP, player)
            }
        }

        isRepairing = false
        originalSlot = -1
        activeRepairSlot = -1
    }

    override fun onDisable() {
        if (isRepairing) {
            restoreItem()
        }
    }
}
