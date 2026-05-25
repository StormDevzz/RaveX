package volthack.modules.combat

import net.minecraft.client.Minecraft
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.Items
import volthack.event.EventBus
import volthack.event.TickEvent
import volthack.setting.Category
import volthack.setting.Module

object BowSpam : Module("BowSpam", "Spams bow shots rapidly", Category.COMBAT) {
    private val drawTicks by int("Draw Ticks", 3, 2, 20, "Number of ticks to draw the bow before releasing")

    init {
        EventBus.listen<TickEvent> { onTick() }
    }

    private fun onTick() {
        if (!enabled) return
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val gameMode = mc.gameMode ?: return

        val isHoldingBow = player.mainHandItem.item == Items.BOW || player.offhandItem.item == Items.BOW
        if (!isHoldingBow) return

        val hand = if (player.mainHandItem.item == Items.BOW) InteractionHand.MAIN_HAND else InteractionHand.OFF_HAND

        if (player.isUsingItem && player.useItem.item == Items.BOW) {
            val ticksUsed = player.ticksUsingItem
            if (ticksUsed >= drawTicks) {
                gameMode.releaseUsingItem(player)
                gameMode.useItem(player, hand)
            }
        }
    }
}
