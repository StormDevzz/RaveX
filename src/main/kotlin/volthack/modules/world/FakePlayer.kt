package volthack.modules.world

import com.mojang.authlib.GameProfile
import net.minecraft.client.Minecraft
import net.minecraft.client.player.RemotePlayer
import net.minecraft.world.entity.Entity
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import volthack.event.EventBus
import volthack.event.PacketSendEvent
import volthack.manager.PacketManager
import volthack.setting.Category
import volthack.setting.Module
import volthack.util.chat.ChatUtils
import java.util.UUID

object FakePlayer : Module("FakePlayer", "Spawns a fake client-side player entity for testing", Category.WORLD) {
    private val nameSetting by text("Name", "FakePlayer", "The fake player's custom name")
    private val copyInventory by boolean("Copy Inventory", true, "Copy your inventory and armor to the fake player")
    private val crits by boolean("Simulate Crits", true, "Spawn critical hit particles when hitting the fake player")

    var fakePlayerEntity: RemotePlayer? = null

    private val sendListener = { event: PacketSendEvent ->
        if (enabled) {
            val packet = event.packet
            if (packet is ServerboundInteractPacket) {
                val targetId = getEntityId(packet)
                if (targetId == -1337) {
                    event.cancelled = true
                    
                    val mc = Minecraft.getInstance()
                    mc.execute {
                        val fp = fakePlayerEntity
                        if (fp != null && fp.isAlive) {
                            fp.hurtTime = 10
                            fp.hurtDuration = 10
                            
                            mc.level?.playSound(
                                mc.player, 
                                fp.x, fp.y, fp.z, 
                                net.minecraft.sounds.SoundEvents.PLAYER_HURT, 
                                net.minecraft.sounds.SoundSource.PLAYERS, 
                                1.0f, 1.0f
                            )
                            
                            if (crits) {
                                for (i in 0..10) {
                                    mc.level?.addParticle(
                                        net.minecraft.core.particles.ParticleTypes.CRIT,
                                        fp.x, fp.y + 1.0, fp.z,
                                        (Math.random() - 0.5) * 0.3,
                                        (Math.random() - 0.5) * 0.3,
                                        (Math.random() - 0.5) * 0.3
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    init {
        PacketManager.registerSendListener(sendListener)
    }

    override fun onEnable() {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val level = mc.level ?: return
        
        val profile = GameProfile(UUID.randomUUID(), nameSetting)
        val fp = RemotePlayer(level, profile)
        fp.setId(-1337)
        fp.setPos(player.x, player.y, player.z)
        fp.setYRot(player.yRot)
        fp.setXRot(player.xRot)
        fp.yHeadRot = player.yHeadRot
        fp.yBodyRot = player.yBodyRot
        
        if (copyInventory) {
            fp.inventory.replaceWith(player.inventory)
        }
        
        level.addEntity(fp)
        fakePlayerEntity = fp
    }

    override fun onDisable() {
        val mc = Minecraft.getInstance()
        val level = mc.level ?: return
        level.removeEntity(-1337, Entity.RemovalReason.DISCARDED)
        fakePlayerEntity = null
    }

    private fun getEntityId(packet: ServerboundInteractPacket): Int {
        return try {
            val field = ServerboundInteractPacket::class.java.getDeclaredField("entityId")
            field.isAccessible = true
            field.get(packet) as Int
        } catch (e: Exception) {
            -1
        }
    }
}
