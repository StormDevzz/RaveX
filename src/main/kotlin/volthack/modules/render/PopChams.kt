package volthack.modules.render

import net.minecraft.client.Minecraft
import net.minecraft.world.entity.player.Player
import volthack.event.EventBus
import volthack.event.Render3DEvent
import volthack.manager.PacketManager
import volthack.setting.Category
import volthack.setting.Module
import volthack.util.render.Render3DUtil
import java.util.concurrent.CopyOnWriteArrayList

object PopChams : Module("PopChams", "Highlights player position when they pop a totem", Category.RENDER) {
    val color by color("Color", 0xFFFF00FF.toInt(), "Highlight color")
    val duration by int("Duration", 40, 10, 100, "Ticks the highlight stays visible")

    private class PopRecord(
        val x: Double,
        val y: Double,
        val z: Double,
        val width: Float,
        val height: Float,
        var ticksRemaining: Int,
        val maxTicks: Int
    )

    private val records = CopyOnWriteArrayList<PopRecord>()

    init {
        EventBus.listen<Render3DEvent> { onRender3D(it) }

        PacketManager.registerReceiveListener { event ->
            val packet = event.packet
            if (enabled && packet::class.java.simpleName == "ClientboundEntityEventPacket") {
                val eventId = try {
                    val f = packet::class.java.getDeclaredField("eventId")
                    f.isAccessible = true
                    f.get(packet) as Byte
                } catch (e: Exception) {
                    try {
                        val m = packet::class.java.getMethod("getEventId")
                        m.invoke(packet) as Byte
                    } catch (e2: Exception) {
                        0.toByte()
                    }
                }

                if (eventId.toInt() == 35) {
                    val entityId = try {
                        val f = packet::class.java.getDeclaredField("entityId")
                        f.isAccessible = true
                        f.get(packet) as Int
                    } catch (e: Exception) {
                        try {
                            val m = packet::class.java.getMethod("getEntityId")
                            m.invoke(packet) as Int
                        } catch (e2: Exception) {
                            -1
                        }
                    }

                    if (entityId != -1) {
                        val mc = Minecraft.getInstance()
                        mc.execute {
                            val entity = mc.level?.getEntity(entityId)
                            if (entity is Player && entity != mc.player) {
                                records.add(
                                    PopRecord(
                                        entity.x,
                                        entity.y,
                                        entity.z,
                                        entity.bbWidth,
                                        entity.bbHeight,
                                        duration,
                                        duration
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun onRender3D(event: Render3DEvent) {
        if (!enabled) return

        val mc = Minecraft.getInstance()
        val cam = mc.gameRenderer.mainCamera.position()

        val col = color
        val r = (col shr 16) and 0xFF
        val g = (col shr 8) and 0xFF
        val b = col and 0xFF

        val iterator = records.iterator()
        while (iterator.hasNext()) {
            val record = iterator.next()
            if (record.ticksRemaining <= 0) {
                records.remove(record)
                continue
            }

            // Fade alpha based on remaining ticks
            val alphaPct = record.ticksRemaining.toFloat() / record.maxTicks
            val alphaVal = (alphaPct * 255).toInt().coerceIn(0, 255)

            Render3DUtil.drawBlockOutline(
                event.modelViewMatrix,
                record.x,
                record.y,
                record.z,
                record.width,
                record.height,
                r, g, b, alphaVal,
                true,
                2.0f
            )

            record.ticksRemaining--
        }
    }

    override fun onDisable() {
        records.clear()
    }
}
