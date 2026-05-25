package volthack.modules.movement

import net.minecraft.client.Minecraft
import net.minecraft.world.entity.ai.attributes.Attributes
import volthack.event.EventBus
import volthack.event.TickEvent
import volthack.setting.Category
import volthack.setting.Module

object Step : Module("Step", "Allows you to step up blocks instantly", Category.MOVEMENT) {
    private val height by float("Height", 1.0f, 0.6f, 2.5f, 0.1f, "The maximum height of blocks you can step up")

    init {
        EventBus.listen<TickEvent> { onTick() }
    }

    private fun onTick() {
        if (!enabled) return
        val player = Minecraft.getInstance().player ?: return
        player.getAttribute(Attributes.STEP_HEIGHT)?.baseValue = height.toDouble()
    }

    override fun onDisable() {
        val player = Minecraft.getInstance().player ?: return
        player.getAttribute(Attributes.STEP_HEIGHT)?.baseValue = 0.6
    }
}
