package volthack.modules.movement

import net.minecraft.client.Minecraft
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.phys.Vec3
import volthack.event.EventBus
import volthack.event.TickEvent
import volthack.setting.Category
import volthack.setting.Module

object LevitCtrl : Module("LevitCtrl", "Allows you to control or disable the levitation effect", Category.MOVEMENT) {
    private val mode by mode("Mode", listOf("Control", "Disable"), "Control", "Control: fly with keys, Disable: fall normally")
    private val speed by float("Speed", 0.3f, 0.05f, 1.0f, 0.05f, "Vertical speed when using Control")
    private val hover by boolean("Hover", true, "Hover in place when no vertical keys are pressed")

    init {
        EventBus.listen<TickEvent> { onTick() }
    }

    private fun onTick() {
        if (!enabled) return
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return

        if (player.hasEffect(MobEffects.LEVITATION)) {
            val delta = player.deltaMovement
            var newY = delta.y

            if (mode == "Control") {
                newY = when {
                    mc.options.keyJump.isDown -> speed.toDouble()
                    mc.options.keyShift.isDown -> -speed.toDouble()
                    hover -> 0.0
                    else -> delta.y
                }
            } else if (mode == "Disable") {
                newY = if (player.onGround()) {
                    0.0
                } else {
                    -0.08
                }
            }

            player.deltaMovement = Vec3(delta.x, newY, delta.z)
        }
    }
}
