package volthack.modules.render

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import volthack.setting.Category
import volthack.setting.Module

object Ambience : Module("Ambience", "Changes screen color ambient overlay", Category.RENDER) {
    val color by color("Color", 0xFF6C63FF.toInt(), "The ambient overlay tint color")
    val alpha by int("Alpha", 50, 0, 255, "Alpha value of the overlay")

    fun renderScreen(context: GuiGraphics) {
        val mc = Minecraft.getInstance()
        val w = mc.window.guiScaledWidth
        val h = mc.window.guiScaledHeight
        val col = color
        val argb = ((alpha and 0xFF) shl 24) or (col and 0x00FFFFFF)
        context.fill(0, 0, w, h, argb)
    }
}
