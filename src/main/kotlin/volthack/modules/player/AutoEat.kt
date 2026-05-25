package volthack.modules.player

import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponents
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.Items
import volthack.event.EventBus
import volthack.event.TickEvent
import volthack.setting.Category
import volthack.setting.Module

object AutoEat : Module("AutoEat", "Automatically eats food when hunger or health is low with advanced filters", Category.PLAYER) {
    private val hungerThreshold by int("Hunger Level", 16, 1, 20)
    private val healthThreshold by int("Health Level", 12, 1, 20)
    private val noGapples by boolean("No Gapples", true, "Do not eat Golden Apples or Enchanted Golden Apples")
    private val noBadFoods by boolean("No Bad Effect Foods", true, "Do not eat Rotten Flesh, Spider Eye, Pufferfish, Poisonous Potato, or Raw Chicken")

    private var originalSlot = -1
    private var isEating = false

    init {
        EventBus.listen<TickEvent> { onTick() }
    }

    private fun onTick() {
        if (!enabled) {
            if (isEating) {
                stopEating()
            }
            return
        }

        val mc = Minecraft.getInstance()
        val player = mc.player ?: return

        val foodLevel = player.foodData.foodLevel
        val health = player.health

        val needsFood = foodLevel <= hungerThreshold || health <= healthThreshold

        if (isEating) {
            // Check if we are done eating or no longer need food
            val currentStack = player.inventory.getItem(player.inventory.selectedSlot)
            if (!needsFood || currentStack.isEmpty || currentStack.get(DataComponents.FOOD) == null) {
                stopEating()
                return
            }

            // Force hold right click/use key
            mc.options.keyUse.isDown = true
            return
        }

        // If we need food, find the best food slot in the hotbar
        if (needsFood) {
            val bestSlot = findFoodSlot()
            if (bestSlot != -1) {
                isEating = true
                originalSlot = player.inventory.selectedSlot
                player.inventory.selectedSlot = bestSlot
                mc.options.keyUse.isDown = true
                mc.gameMode?.useItem(player, InteractionHand.MAIN_HAND)
            }
        }
    }

    private fun findFoodSlot(): Int {
        val player = Minecraft.getInstance().player ?: return -1
        val inv = player.inventory
        
        for (i in 0..8) {
            val stack = inv.getItem(i)
            if (stack.isEmpty || stack.get(DataComponents.FOOD) == null) continue

            val item = stack.item

            // Filter out Golden Apples if blocked
            if (noGapples && (item == Items.GOLDEN_APPLE || item == Items.ENCHANTED_GOLDEN_APPLE)) {
                continue
            }

            // Filter out bad effect foods
            if (noBadFoods && (
                item == Items.ROTTEN_FLESH ||
                item == Items.SPIDER_EYE ||
                item == Items.PUFFERFISH ||
                item == Items.POISONOUS_POTATO ||
                item == Items.CHICKEN
            )) {
                continue
            }

            return i
        }
        return -1
    }

    private fun stopEating() {
        val mc = Minecraft.getInstance()
        val player = mc.player
        
        isEating = false
        mc.options.keyUse.isDown = false

        if (originalSlot in 0..8 && player != null) {
            player.inventory.selectedSlot = originalSlot
        }
        originalSlot = -1
    }

    override fun onDisable() {
        stopEating()
    }
}
