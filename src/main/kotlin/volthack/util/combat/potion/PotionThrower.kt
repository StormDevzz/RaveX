package volthack.util.combat.potion

import net.minecraft.client.Minecraft
import net.minecraft.world.InteractionHand
import volthack.manager.RotationManager

object PotionThrower {
    /**
     * Throws the splash potion in the given slot under the player.
     * Restores the previous hotbar slot and player rotation.
     */
    fun throwPotion(slot: Int) {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val gameMode = mc.gameMode ?: return
        
        if (slot < 0 || slot > 8) return
        
        val prevSlot = player.inventory.selectedSlot
        
        // 1. Swap slot
        player.inventory.selectedSlot = slot
        
        // 2. Setup down rotation
        val originalYaw = player.yRot
        RotationManager.setRotations(originalYaw, 90.0f, true)
        
        // 3. Throw potion under ourselves
        RotationManager.runRotation {
            gameMode.useItem(player, InteractionHand.MAIN_HAND)
        }
        
        // 4. Restore original state
        player.inventory.selectedSlot = prevSlot
        RotationManager.reset()
    }
}
