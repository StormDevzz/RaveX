package volthack.util.combat.potion

import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponents
import net.minecraft.world.effect.MobEffect
import net.minecraft.core.Holder
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

object PotionFinder {
    /**
     * Finds a splash/lingering potion slot in the player's hotbar that has the specified mob effect.
     * Returns -1 if not found.
     */
    fun findSplashPotion(effect: Holder<MobEffect>): Int {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return -1
        
        for (i in 0..8) {
            val stack = player.inventory.getItem(i)
            if (stack.isEmpty) continue
            if (stack.item == Items.SPLASH_POTION || stack.item == Items.LINGERING_POTION) {
                if (hasEffect(stack, effect)) {
                    return i
                }
            }
        }
        return -1
    }

    /**
     * Finds a splash/lingering potion slot in the player's main inventory (slots 9..35).
     * Returns -1 if not found.
     */
    fun findSplashPotionInventory(effect: Holder<MobEffect>): Int {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return -1
        
        for (i in 9..35) {
            val stack = player.inventory.getItem(i)
            if (stack.isEmpty) continue
            if (stack.item == Items.SPLASH_POTION || stack.item == Items.LINGERING_POTION) {
                if (hasEffect(stack, effect)) {
                    return i
                }
            }
        }
        return -1
    }

    fun hasEffect(stack: ItemStack, effect: Holder<MobEffect>): Boolean {
        val contents = stack.get(DataComponents.POTION_CONTENTS) ?: return false
        
        val potionOpt = contents.potion()
        if (potionOpt.isPresent) {
            val potion = potionOpt.get().value()
            for (eff in potion.getEffects()) {
                if (eff.effect == effect) {
                    return true
                }
            }
        }
        
        for (eff in contents.customEffects()) {
            if (eff.effect == effect) {
                return true
            }
        }
        
        return false
    }
}
