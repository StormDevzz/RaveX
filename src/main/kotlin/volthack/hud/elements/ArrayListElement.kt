package volthack.hud.elements

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import volthack.gui.font.GUIFontRenderer
import volthack.gui.theme.VoltHackTheme
import volthack.hud.HUDElement
import volthack.setting.ModuleManager

class ArrayListElement : HUDElement("ArrayList") {
    companion object {
        private const val PADDING = 4
        private const val GAP = 2
    }

    private data class ModuleEntry(val name: String, val width: Int)
    private var cached: List<ModuleEntry> = emptyList()
    private var cachedScreenW = 0
    private var lastEnabledCount = -1

    init {
        x = 600
        y = 10
        cachedWidth = 100
        cachedHeight = 150
        enabled = false
    }

    private fun rebuild() {
        val enabled = ModuleManager.getAll().filter { it.enabled }
        if (enabled.isEmpty()) {
            cached = emptyList()
            cachedWidth = 0
            cachedHeight = 0
            return
        }
        cached = enabled.map { ModuleEntry(it.name, GUIFontRenderer.width(it.name)) }
            .sortedByDescending { it.width }
        cachedScreenW = Minecraft.getInstance().window.guiScaledWidth
    }

    override fun draw(ctx: GuiGraphics) {
        val count = ModuleManager.getAll().count { it.enabled }
        if (count != lastEnabledCount) {
            lastEnabledCount = count
            rebuild()
        }

        if (cached.isEmpty()) {
            cachedWidth = 0
            cachedHeight = 0
            return
        }

        val screenWidth = Minecraft.getInstance().window.guiScaledWidth
        if (screenWidth != cachedScreenW) {
            cachedScreenW = screenWidth
            rebuild()
        }

        var maxW = 0
        for (entry in cached) {
            val w = entry.width + PADDING * 2 + 4
            if (w > maxW) maxW = w
        }
        cachedWidth = maxW

        var cy = y
        for (entry in cached) {
            val cx = screenWidth - cachedWidth - PADDING
            ctx.fill(cx, cy, cx + cachedWidth, cy + GUIFontRenderer.height + PADDING, VoltHackTheme.surface)
            ctx.fill(cx, cy, cx + 2, cy + GUIFontRenderer.height + PADDING, VoltHackTheme.accent)
            GUIFontRenderer.draw(ctx, entry.name, (cx + PADDING + 2).toFloat(), (cy + 2).toFloat(), VoltHackTheme.textPrimary)
            cy += GUIFontRenderer.height + PADDING + GAP
        }

        cachedHeight = cy - y
    }
}
