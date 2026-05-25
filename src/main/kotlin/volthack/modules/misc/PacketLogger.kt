package volthack.modules.misc

import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket
import net.minecraft.network.protocol.common.ServerboundPongPacket
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket
import net.minecraft.network.protocol.common.ClientboundPingPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket
import volthack.event.PacketReceiveEvent
import volthack.event.PacketSendEvent
import volthack.manager.PacketManager
import volthack.setting.Category
import volthack.setting.Module
import volthack.util.chat.ChatUtils

object PacketLogger : Module("PacketLogger", "Logs sent and received network packets", Category.MISC) {
    private val logSent by boolean("Log Sent", true, "Log outgoing packets")
    private val logReceived by boolean("Log Received", false, "Log incoming packets")
    private val chatLog by boolean("Log to Chat", true, "Print packets to client chat")
    private val consoleLog by boolean("Log to Console", false, "Print packets to system logs")
    private val ignoreMovement by boolean("Ignore Movement", true, "Ignore player movement packets")
    private val ignoreKeepAlive by boolean("Ignore KeepAlive", true, "Ignore keep-alive, ping, and pong packets")

    private val sendListener = { event: PacketSendEvent ->
        if (enabled && logSent) {
            val packet = event.packet
            if (shouldLog(packet)) {
                logPacket("Sent", packet::class.java.simpleName)
            }
        }
    }

    private val receiveListener = { event: PacketReceiveEvent ->
        if (enabled && logReceived) {
            val packet = event.packet
            if (shouldLog(packet)) {
                logPacket("Received", packet::class.java.simpleName)
            }
        }
    }

    init {
        PacketManager.registerSendListener(sendListener)
        PacketManager.registerReceiveListener(receiveListener)
    }

    private fun shouldLog(packet: Any): Boolean {
        if (ignoreMovement && (packet is ServerboundMovePlayerPacket || packet is ClientboundMoveEntityPacket)) {
            return false
        }
        if (ignoreKeepAlive && (
            packet is ServerboundKeepAlivePacket || 
            packet is ServerboundPongPacket ||
            packet is ClientboundKeepAlivePacket ||
            packet is ClientboundPingPacket
        )) {
            return false
        }
        return true
    }

    private fun logPacket(direction: String, name: String) {
        val dirColor = if (direction == "Sent") "§a" else "§c"
        val message = "§7[§6PacketLogger§7] $dirColor$direction: §f$name"
        
        if (chatLog) {
            ChatUtils.addChatMessage(message)
        }
        if (consoleLog) {
            println("[PacketLogger] $direction: $name")
        }
    }
}
