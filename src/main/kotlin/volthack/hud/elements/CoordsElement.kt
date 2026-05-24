package volthack.hud.elements

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import volthack.gui.font.GUIFontRenderer
import volthack.gui.theme.VoltHackTheme
import volthack.hud.HUDElement

class CoordsElement : HUDElement("Coords") {
    private val showNether by boolean("Show Nether", true)
    private val customColor by color("Color", 0xFF6C63FF.toInt())

    init {
        x = 4
        y = 200
        cachedWidth = 150
        cachedHeight = 22
        enabled = false
    }

    override fun render(ctx: GuiGraphics) {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return

        val cx = "%.1f".format(player.x)
        val cy = "%.1f".format(player.y)
        val cz = "%.1f".format(player.z)

        val coordsText = "XYZ: $cx, $cy, $cz"
        GUIFontRenderer.draw(ctx, coordsText, x.toFloat(), y.toFloat(), customColor)

        var h = GUIFontRenderer.height
        if (showNether) {
            val inNether = mc.level?.dimension()?.toString()?.contains("nether") == true
            val mult = if (inNether) 8.0 else 0.125
            val netherX = "%.1f".format(player.x * mult)
            val netherZ = "%.1f".format(player.z * mult)
            val netherText = "Nether: $netherX, $cy, $netherZ"
            GUIFontRenderer.draw(ctx, netherText, x.toFloat(), (y + h + 2).toFloat(), VoltHackTheme.textSecondary)
            h += GUIFontRenderer.height + 2
        }

        cachedWidth = maxOf(GUIFontRenderer.width(coordsText), if (showNether) GUIFontRenderer.width("Nether: -9999.9, 999.9, -9999.9") else 0).coerceAtLeast(80)
        cachedHeight = h
    }
}
