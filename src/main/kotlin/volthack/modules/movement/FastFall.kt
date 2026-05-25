package volthack.modules.movement

import net.minecraft.client.Minecraft
import net.minecraft.world.phys.Vec3
import volthack.event.EventBus
import volthack.event.TickEvent
import volthack.setting.Category
import volthack.setting.Module

object FastFall : Module("FastFall", "Allows you to fall and descend off blocks faster", Category.MOVEMENT) {
    private val modeSetting by mode("Mode", listOf("Normal", "Instant"), "Normal", "Fast fall execution mode")
    private val speed by float("Speed", 0.5f, 0.1f, 3.0f, 0.1f, "Speed modifier in Normal mode")
    private val distance by float("Max Distance", 4.0f, 1.0f, 10.0f, 0.5f, "Snapping distance in Instant mode")

    init {
        EventBus.listen<TickEvent> { onTick() }
    }

    private fun onTick() {
        if (!enabled) return
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        
        if (player.isInWater || player.isInLava || player.onClimbable() || player.abilities.flying) return
        if (mc.options.keyJump.isDown) return
        
        if (!player.onGround() && player.deltaMovement.y < 0.0) {
            if (modeSetting == "Normal") {
                player.deltaMovement = player.deltaMovement.add(0.0, -speed.toDouble(), 0.0)
            } else if (modeSetting == "Instant") {
                // Raycast downwards to find ground
                val level = mc.level ?: return
                var groundY = player.y
                var foundGround = false
                while (groundY > player.y - distance) {
                    val pos = net.minecraft.core.BlockPos.containing(player.x, groundY - 0.05, player.z)
                    if (!level.getBlockState(pos).isAir) {
                        foundGround = true
                        break
                    }
                    groundY -= 0.05
                }
                
                if (foundGround && player.y - groundY < distance) {
                    player.setPos(player.x, groundY, player.z)
                    player.deltaMovement = Vec3(player.deltaMovement.x, -1.0, player.deltaMovement.z)
                }
            }
        }
    }
}
