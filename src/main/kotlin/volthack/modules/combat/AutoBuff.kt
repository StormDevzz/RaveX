package volthack.modules.combat

import net.minecraft.client.Minecraft
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffects
import net.minecraft.core.Holder
import net.minecraft.world.inventory.ClickType
import volthack.event.EventBus
import volthack.event.TickEvent
import volthack.setting.Category
import volthack.setting.Module
import volthack.util.combat.potion.PotionEffectChecker
import volthack.util.combat.potion.PotionFinder
import volthack.util.combat.potion.PotionThrower
import volthack.util.player.InventoryUtils

object AutoBuff : Module("AutoBuff", "Automatically throws splash potions under you", Category.COMBAT) {
    private val speed by boolean("Speed", true, "Auto throw Speed potions")
    private val strength by boolean("Strength", true, "Auto throw Strength potions")
    private val invisibility by boolean("Invisibility", false, "Auto throw Invisibility potions")
    private val fireRes by boolean("Fire Resistance", false, "Auto throw Fire Resistance potions")
    private val delay by int("Delay (Ticks)", 20, 5, 100, "Tick delay between throwing potions")
    
    private val autoSwapObj = boolean("Auto Swap", true, "Swap potions from inventory if not in hotbar")
    val autoSwap by autoSwapObj
    
    private val swapSlotObj = int("Swap Slot", 8, 0, 8, "Hotbar slot to swap potions into")
    val swapSlot by swapSlotObj

    private var tickCounter = 0

    init {
        EventBus.listen<TickEvent> { onTick() }
        swapSlotObj.showIf { autoSwap }
    }

    private fun onTick() {
        if (!enabled) return
        val mc = Minecraft.getInstance()
        if (mc.player == null) return

        tickCounter++
        if (tickCounter < delay) return

        // Speed
        if (speed && PotionEffectChecker.shouldBuff(MobEffects.SPEED)) {
            if (tryBuff(MobEffects.SPEED)) {
                tickCounter = 0
                return
            }
        }

        // Strength
        if (strength && PotionEffectChecker.shouldBuff(MobEffects.STRENGTH)) {
            if (tryBuff(MobEffects.STRENGTH)) {
                tickCounter = 0
                return
            }
        }

        // Invisibility
        if (invisibility && PotionEffectChecker.shouldBuff(MobEffects.INVISIBILITY)) {
            if (tryBuff(MobEffects.INVISIBILITY)) {
                tickCounter = 0
                return
            }
        }

        // Fire Resistance
        if (fireRes && PotionEffectChecker.shouldBuff(MobEffects.FIRE_RESISTANCE)) {
            if (tryBuff(MobEffects.FIRE_RESISTANCE)) {
                tickCounter = 0
                return
            }
        }
    }

    private fun tryBuff(effect: Holder<MobEffect>): Boolean {
        // 1. Search in hotbar
        val hotbarSlot = PotionFinder.findSplashPotion(effect)
        if (hotbarSlot != -1) {
            PotionThrower.throwPotion(hotbarSlot)
            return true
        }

        // 2. Search in inventory if AutoSwap is enabled
        if (autoSwap) {
            val invSlot = PotionFinder.findSplashPotionInventory(effect)
            if (invSlot != -1) {
                // In InventoryMenu, slots 9..35 are the main inventory.
                // In mc.player.containerMenu, these same slots correspond to container slots 9..35!
                // To swap inventory container slot `invSlot` with hotbar slot `swapSlot`:
                InventoryUtils.click(invSlot, swapSlot, ClickType.SWAP)
                
                // Throw it
                PotionThrower.throwPotion(swapSlot)
                
                // Swap it back to restore inventory cleanliness
                InventoryUtils.click(invSlot, swapSlot, ClickType.SWAP)
                return true
            }
        }
        return false
    }

    override fun onEnable() {
        tickCounter = 0
    }
}
