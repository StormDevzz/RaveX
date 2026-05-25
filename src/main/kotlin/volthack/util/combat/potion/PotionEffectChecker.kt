package volthack.util.combat.potion

import net.minecraft.client.Minecraft
import net.minecraft.world.effect.MobEffect
import net.minecraft.core.Holder

object PotionEffectChecker {
    /**
     * Checks if the player needs to be buffed with the specified effect.
     * Returns true if the player doesn't have the effect, or if the remaining duration is low.
     */
    fun shouldBuff(effect: Holder<MobEffect>, minDurationTicks: Int = 30): Boolean {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return false
        
        val activeEffect = player.getEffect(effect) ?: return true
        return activeEffect.duration <= minDurationTicks
    }
}
