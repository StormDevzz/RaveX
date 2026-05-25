package volthack.modules.misc

import net.minecraft.client.multiplayer.PlayerInfo
import net.minecraft.network.chat.Component
import volthack.setting.Category
import volthack.setting.Module

object TabUtils : Module("TabUtils", "Allows changing tab list appearance, shows ping/health, and protects VIP status styling", Category.MISC) {
    val showPing by boolean("Show Ping", true)
    val showHealth by boolean("Show Health", true)

    fun getTabName(playerInfo: PlayerInfo, original: Component?): Component {
        if (!enabled) return original ?: Component.literal(playerInfo.profile.name)

        // "главное если будет донат у игрока, не будет менять его"
        // If the server has set a custom display name (VIP, Premium, Admin prefixes/colors), we preserve it exactly!
        if (original != null) {
            return original
        }

        val baseName = playerInfo.profile.name
        var resultText = baseName

        // Append Ping
        if (showPing) {
            val ping = playerInfo.latency
            val pingColor = when {
                ping < 50 -> "§a" // Green
                ping < 150 -> "§e" // Yellow
                else -> "§c" // Red
            }
            resultText += " ${pingColor}[${ping}ms]§r"
        }

        // Append Health if player exists in world
        if (showHealth) {
            val mc = net.minecraft.client.Minecraft.getInstance()
            val player = mc.level?.players()?.find { it.gameProfile.id == playerInfo.profile.id }
            if (player != null) {
                val hp = player.health.toInt()
                val hpColor = when {
                    hp > 15 -> "§a"
                    hp > 7 -> "§e"
                    else -> "§c"
                }
                resultText += " ${hpColor}[${hp} HP]§r"
            }
        }

        return Component.literal(resultText)
    }
}
