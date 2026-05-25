package volthack.modules.render

import volthack.setting.Category
import volthack.setting.Module

object Chams : Module("Chams", "Highlights players and crystals through walls", Category.RENDER) {
    val players by boolean("Players", true, "Enable Chams for player models")
    val playerColor by color("Player Color", 0xFFFF0000.toInt(), "Chams color for players")
    val playerAlpha by int("Player Alpha", 128, 0, 255, "Chams opacity for players")

    val crystals by boolean("Crystals", true, "Enable Chams for End Crystals")
    val crystalColor by color("Crystal Color", 0xFF00FF00.toInt(), "Chams color for End Crystals")
    val crystalAlpha by int("Crystal Alpha", 128, 0, 255, "Chams opacity for End Crystals")

    var renderingPlayer = false
    var renderingCrystal = false

    fun getChamsColor(originalColor: Int): Int {
        if (!enabled) return originalColor

        if (renderingPlayer && players) {
            val c = playerColor
            val a = playerAlpha
            return ((a and 0xFF) shl 24) or (c and 0x00FFFFFF)
        }

        if (renderingCrystal && crystals) {
            val c = crystalColor
            val a = crystalAlpha
            return ((a and 0xFF) shl 24) or (c and 0x00FFFFFF)
        }

        return originalColor
    }
}
