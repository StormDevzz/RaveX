package volthack.modules.player

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.DeathScreen
import volthack.event.EventBus
import volthack.event.TickEvent
import volthack.setting.Category
import volthack.setting.Module

object AutoRespawn : Module("AutoRespawn", "Automatically respawns you immediately after you die", Category.PLAYER) {
    init {
        EventBus.listen<TickEvent> { onTick() }
    }

    private fun onTick() {
        if (!enabled) return

        val mc = Minecraft.getInstance()
        val player = mc.player ?: return

        // If the death screen is open, respawn immediately
        if (mc.screen is DeathScreen) {
            player.respawn()
            mc.setScreen(null)
        }
    }
}
