package volthack.gui.component

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import org.lwjgl.glfw.GLFW
import volthack.gui.font.GUIFontRenderer
import volthack.gui.theme.VoltHackTheme
import volthack.setting.Setting
import kotlin.math.roundToInt

object SettingWidget {
    private var dragging: Dragging? = null
    var hoveredSetting: Setting<*>? = null
    
    var activeInputSetting: Setting<*>? = null
    var inputText: String = ""
    private var inputAnim = 0f

    var expandedColorSetting: Setting.Color? = null
    var expandedModeSetting: Setting.Mode? = null

    private val PRESET_COLORS = intArrayOf(
        0xFFFF6B6B.toInt(), // Soft Red
        0xFFFF9F43.toInt(), // Sunset Orange
        0xFFFECA57.toInt(), // Gold Yellow
        0xFF1DD1A1.toInt(), // Emerald Green
        0xFF00D2D3.toInt(), // Electric Teal
        0xFF54A0FF.toInt(), // Sky Blue
        0xFF5F27CD.toInt(), // Royal Purple
        0xFFFF9FF3.toInt()  // Sakura Pink
    )

    private data class Dragging(
        val setting: Setting<*>,
        val startMouseX: Int,
        val startValue: Float,
        val sliderX: Int,
        val sliderW: Int,
        val type: String = "slider"
    )

    fun getHeight(setting: Setting<*>): Int {
        if (!setting.isVisible()) return 0
        return when (setting) {
            is Setting.Float, is Setting.Int -> 32
            is Setting.Color -> if (expandedColorSetting == setting) 94 else 26
            is Setting.Mode -> if (expandedModeSetting == setting) 26 + setting.modes.size * 18 else 26
            else -> 26
        }
    }

    @JvmOverloads
    fun render(ctx: GuiGraphics, setting: Setting<*>, x: Int, y: Int, width: Int, mouseX: Int = -999, mouseY: Int = -999) {
        if (!setting.isVisible()) return

        val height = getHeight(setting)

        // Check hover for tooltip
        if (mouseX in x..x + width && mouseY in y..y + height) {
            hoveredSetting = setting
            // Lose focus on text box if mouse clicks elsewhere
            checkFocusLoss(mouseX, mouseY, x, y, width, height)
        }

        when (setting) {
            is Setting.Boolean -> renderBoolean(ctx, setting, x, y, width)
            is Setting.Float -> renderFloat(ctx, setting, x, y, width)
            is Setting.Int -> renderInt(ctx, setting, x, y, width)
            is Setting.Mode -> renderMode(ctx, setting, x, y, width)
            is Setting.Color -> renderColor(ctx, setting, x, y, width)
            is Setting.StringSetting -> renderString(ctx, setting, x, y, width)
        }
    }

    fun mouseClicked(mouseX: Int, mouseY: Int, setting: Setting<*>, x: Int, y: Int, width: Int, button: Int = 0): Boolean {
        if (!setting.isVisible()) return false
        
        // Handle right-click on float/int to activate text input
        if (button == 1 && (setting is Setting.Float || setting is Setting.Int)) {
            if (mouseX in x..x + width && mouseY in y..y + 32) {
                activeInputSetting = setting
                inputText = if (setting is Setting.Float) "%.2f".format(setting.value) else setting.value.toString()
                inputAnim = 0f
                return true
            }
        }

        return when (setting) {
            is Setting.Boolean -> clickBoolean(mouseX, mouseY, setting, x, y, width)
            is Setting.Float -> clickFloat(mouseX, mouseY, setting, x, y, width)
            is Setting.Int -> clickInt(mouseX, mouseY, setting, x, y, width)
            is Setting.Mode -> clickMode(mouseX, mouseY, setting, x, y, width, button)
            is Setting.Color -> clickColor(mouseX, mouseY, setting, x, y, width)
            is Setting.StringSetting -> false
            else -> false
        }
    }

    fun mouseReleased(): Boolean {
        if (dragging != null) {
            dragging = null
            return true
        }
        return false
    }

    fun mouseDragged(mx: Int, my: Int, button: Int): Boolean {
        val drag = dragging ?: return false
        val s = drag.setting
        val ratio = ((mx - drag.sliderX).toFloat() / drag.sliderW).coerceIn(0f, 1f)
        
        when (s) {
            is Setting.Float -> {
                val range = s.max - s.min
                val steps = (range / s.step).roundToInt().coerceAtLeast(1)
                val stepped = (ratio * steps).roundToInt().coerceIn(0, steps)
                s.value = s.min + stepped * s.step
            }
            is Setting.Int -> {
                val range = s.max - s.min
                s.value = (s.min + (ratio * range).roundToInt()).coerceIn(s.min, s.max)
            }
            is Setting.Color -> {
                val hsb = FloatArray(3)
                val r = (s.value shr 16) and 0xFF
                val g = (s.value shr 8) and 0xFF
                val b = s.value and 0xFF
                java.awt.Color.RGBtoHSB(r, g, b, hsb)
                if (drag.type == "color_hue") {
                    s.value = java.awt.Color.HSBtoRGB(ratio, hsb[1], hsb[2])
                } else if (drag.type == "color_bri") {
                    s.value = java.awt.Color.HSBtoRGB(hsb[0], hsb[1], ratio)
                }
            }
            else -> {}
        }
        return true
    }

    fun keyPressed(event: KeyEvent): Boolean {
        val s = activeInputSetting ?: return false
        val key = event.key()
        
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            applyInput()
            activeInputSetting = null
            return true
        } else if (key == GLFW.GLFW_KEY_ESCAPE) {
            activeInputSetting = null
            return true
        } else if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (inputText.isNotEmpty()) {
                inputText = inputText.substring(0, inputText.length - 1)
            }
            return true
        }
        return false
    }

    fun charTyped(event: CharacterEvent): Boolean {
        val char = event.codepoint().toChar()
        if (char.isDigit() || char == '.' || char == '-') {
            if (inputText.length < 10) {
                inputText += char
            }
            return true
        }
        return false
    }

    private fun applyInput() {
        val s = activeInputSetting ?: return
        try {
            when (s) {
                is Setting.Float -> {
                    val parsed = inputText.toFloatOrNull()
                    if (parsed != null) {
                        s.value = parsed.coerceIn(s.min, s.max)
                    }
                }
                is Setting.Int -> {
                    val parsed = inputText.toIntOrNull()
                    if (parsed != null) {
                        s.value = parsed.coerceIn(s.min, s.max)
                    }
                }
                else -> {}
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun resetState() {
        activeInputSetting = null
        inputText = ""
        dragging = null
        hoveredSetting = null
        expandedColorSetting = null
        expandedModeSetting = null
    }

    private fun checkFocusLoss(mx: Int, my: Int, x: Int, y: Int, w: Int, h: Int) {
        val active = activeInputSetting ?: return
        // If mouse is outside this setting widget, apply changes
        if (mx !in x..x + w || my !in y..y + h) {
            applyInput()
            activeInputSetting = null
        }
    }

    private fun renderBoolean(ctx: GuiGraphics, s: Setting.Boolean, x: Int, y: Int, width: Int) {
        val color = if (s.value) VoltHackTheme.accent else VoltHackTheme.textDisabled
        ctx.fill(x, y, x + width, y + 26, 0x10000000.toInt())
        GUIFontRenderer.draw(ctx, s.name, (x + 4).toFloat(), (y + 7).toFloat(), VoltHackTheme.textPrimary)

        val toggleX = x + width - 28
        val toggleY = y + 5
        val toggleW = 24
        val toggleH = 14
        ctx.fill(toggleX, toggleY, toggleX + toggleW, toggleY + toggleH, color)
        val knobX = if (s.value) toggleX + toggleW - 10 else toggleX + 2
        ctx.fill(knobX, toggleY + 2, knobX + 8, toggleY + toggleH - 2, 0xFFFFFFFF.toInt())
    }

    private fun renderFloat(ctx: GuiGraphics, s: Setting.Float, x: Int, y: Int, width: Int) {
        ctx.fill(x, y, x + width, y + 32, 0x10000000.toInt())
        GUIFontRenderer.draw(ctx, s.name, (x + 4).toFloat(), (y + 3).toFloat(), VoltHackTheme.textSecondary)

        val label = "%.2f".format(s.value)
        GUIFontRenderer.draw(ctx, label, (x + width - GUIFontRenderer.width(label) - 4).toFloat(),
            (y + 3).toFloat(), VoltHackTheme.accent)

        val slY = y + 20
        val slH = 4

        if (s == activeInputSetting) {
            inputAnim += (1f - inputAnim) * 0.18f
            val boxColor = VoltHackTheme.surfaceLight
            val borderC = VoltHackTheme.accent
            
            // Draw animated text box instead of slider
            ctx.fill(x + 2, slY - 4, x + width - 2, slY + slH + 4, boxColor)
            val bt = 1
            ctx.fill(x + 2, slY - 4, x + width - 2, slY - 4 + bt, borderC)
            ctx.fill(x + 2, slY + slH + 4 - bt, x + width - 2, slY + slH + 4, borderC)
            ctx.fill(x + 2, slY - 4, x + 2 + bt, slY + slH + 4, borderC)
            ctx.fill(x + width - 2 - bt, slY - 4, x + width - 2, slY + slH + 4, borderC)

            val cursor = if ((System.currentTimeMillis() / 500) % 2 == 0L) "|" else ""
            GUIFontRenderer.draw(ctx, inputText + cursor, (x + 6).toFloat(), (slY - 2).toFloat(), VoltHackTheme.textPrimary)
        } else {
            ctx.fill(x + 2, slY, x + width - 2, slY + slH, VoltHackTheme.surfaceLight)

            val fill = ((s.value - s.min) / (s.max - s.min) * (width - 4)).coerceIn(0f, (width - 4).toFloat())
            val fillW = fill.roundToInt()
            ctx.fill(x + 2, slY, x + 2 + fillW, slY + slH, VoltHackTheme.accent)

            val thumbX = (x + 2 + fillW).coerceIn(x + 1, x + width - 2) - 3
            ctx.fill(thumbX, slY - 2, thumbX + 6, slY + slH + 2, 0xFFFFFFFF.toInt())
        }
    }

    private fun renderInt(ctx: GuiGraphics, s: Setting.Int, x: Int, y: Int, width: Int) {
        ctx.fill(x, y, x + width, y + 32, 0x10000000.toInt())
        GUIFontRenderer.draw(ctx, s.name, (x + 4).toFloat(), (y + 3).toFloat(), VoltHackTheme.textSecondary)

        val label = s.value.toString()
        GUIFontRenderer.draw(ctx, label, (x + width - GUIFontRenderer.width(label) - 4).toFloat(),
            (y + 3).toFloat(), VoltHackTheme.accent)

        val slY = y + 20
        val slH = 4

        if (s == activeInputSetting) {
            inputAnim += (1f - inputAnim) * 0.18f
            val boxColor = VoltHackTheme.surfaceLight
            val borderC = VoltHackTheme.accent
            
            // Draw animated text box instead of slider
            ctx.fill(x + 2, slY - 4, x + width - 2, slY + slH + 4, boxColor)
            val bt = 1
            ctx.fill(x + 2, slY - 4, x + width - 2, slY - 4 + bt, borderC)
            ctx.fill(x + 2, slY + slH + 4 - bt, x + width - 2, slY + slH + 4, borderC)
            ctx.fill(x + 2, slY - 4, x + 2 + bt, slY + slH + 4, borderC)
            ctx.fill(x + width - 2 - bt, slY - 4, x + width - 2, slY + slH + 4, borderC)

            val cursor = if ((System.currentTimeMillis() / 500) % 2 == 0L) "|" else ""
            GUIFontRenderer.draw(ctx, inputText + cursor, (x + 6).toFloat(), (slY - 2).toFloat(), VoltHackTheme.textPrimary)
        } else {
            ctx.fill(x + 2, slY, x + width - 2, slY + slH, VoltHackTheme.surfaceLight)

            val range = (s.max - s.min).coerceAtLeast(1)
            val fill = (s.value - s.min).toFloat() / range * (width - 4)
            val fillW = fill.roundToInt()
            ctx.fill(x + 2, slY, x + 2 + fillW, slY + slH, VoltHackTheme.accent)

            val thumbX = (x + 2 + fillW).coerceIn(x + 1, x + width - 2) - 3
            ctx.fill(thumbX, slY - 2, thumbX + 6, slY + slH + 2, 0xFFFFFFFF.toInt())
        }
    }

    private fun renderMode(ctx: GuiGraphics, s: Setting.Mode, x: Int, y: Int, width: Int) {
        val isExpanded = expandedModeSetting == s
        val h = if (isExpanded) 26 + s.modes.size * 18 else 26
        
        ctx.fill(x, y, x + width, y + h, 0x10000000.toInt())
        GUIFontRenderer.draw(ctx, s.name, (x + 4).toFloat(), (y + 4).toFloat(), VoltHackTheme.textSecondary)
        val idx = s.modes.indexOf(s.value).coerceAtLeast(0)
        GUIFontRenderer.draw(ctx, "${idx + 1}/${s.modes.size}", (x + width - 32).toFloat(),
            (y + 4).toFloat(), VoltHackTheme.textDisabled)
        GUIFontRenderer.draw(ctx, s.value, (x + 4).toFloat(), (y + 14).toFloat(), VoltHackTheme.accent)
        
        if (isExpanded) {
            for (i in s.modes.indices) {
                val m = s.modes[i]
                val mY = y + 26 + i * 18
                val isCurrent = m == s.value
                val slotBg = if (isCurrent) VoltHackTheme.accentGlow else 0x1A000000.toInt()
                ctx.fill(x + 4, mY, x + width - 4, mY + 16, slotBg)
                
                val textColor = if (isCurrent) VoltHackTheme.accent else VoltHackTheme.textSecondary
                GUIFontRenderer.draw(ctx, m, (x + 8).toFloat(), (mY + 4).toFloat(), textColor)
            }
        }
    }

    private fun renderColor(ctx: GuiGraphics, s: Setting.Color, x: Int, y: Int, width: Int) {
        val isExpanded = expandedColorSetting == s
        val h = if (isExpanded) 94 else 26
        
        ctx.fill(x, y, x + width, y + h, 0x10000000.toInt())
        GUIFontRenderer.draw(ctx, s.name, (x + 4).toFloat(), (y + 7).toFloat(), VoltHackTheme.textPrimary)
        
        // Header color box
        ctx.fill(x + width - 22, y + 4, x + width - 6, y + 20, s.value)
        ctx.fill(x + width - 22, y + 4, x + width - 6, y + 20, 0x40000000.toInt())
        
        if (isExpanded) {
            // 1. Draw curated presets grid
            val presetY = y + 32
            for (i in PRESET_COLORS.indices) {
                val pX = x + 6 + i * 20
                ctx.fill(pX, presetY, pX + 14, presetY + 14, PRESET_COLORS[i])
                // Highlight if selected
                if (s.value == PRESET_COLORS[i]) {
                    val border = 0xFFFFFFFF.toInt()
                    ctx.fill(pX - 1, presetY - 1, pX + 15, presetY, border)
                    ctx.fill(pX - 1, presetY + 14, pX + 15, presetY + 15, border)
                    ctx.fill(pX - 1, presetY, pX, presetY + 14, border)
                    ctx.fill(pX + 14, presetY, pX + 15, presetY + 14, border)
                }
            }
            
            // Convert current color to HSB for sliders
            val hsb = FloatArray(3)
            val r = (s.value shr 16) and 0xFF
            val g = (s.value shr 8) and 0xFF
            val b = s.value and 0xFF
            java.awt.Color.RGBtoHSB(r, g, b, hsb)
            val currentHue = hsb[0]
            val currentBri = hsb[2]
            
            val barX = x + 6
            val barW = width - 12
            
            // 2. Draw Hue Slider (Rainbow)
            val barY = y + 54
            val barH = 8
            val slices = 32
            val sliceW = barW.toFloat() / slices
            for (i in 0 until slices) {
                val hueVal = i.toFloat() / slices
                val rgb = java.awt.Color.HSBtoRGB(hueVal, 1.0f, 1.0f)
                ctx.fill((barX + i * sliceW).toInt(), barY, (barX + (i + 1) * sliceW).toInt(), barY + barH, rgb)
            }
            // Hue Handle
            val thumbX = barX + (currentHue * barW).toInt()
            ctx.fill(thumbX - 2, barY - 2, thumbX + 2, barY + barH + 2, 0xFFFFFFFF.toInt())
            
            // 3. Draw Brightness Slider (Color to Black/White gradient)
            val briY = y + 72
            val briH = 8
            for (i in 0 until slices) {
                val briVal = i.toFloat() / slices
                val rgb = java.awt.Color.HSBtoRGB(currentHue, hsb[1], briVal)
                ctx.fill((barX + i * sliceW).toInt(), briY, (barX + (i + 1) * sliceW).toInt(), briY + briH, rgb)
            }
            // Brightness Handle
            val briThumbX = barX + (currentBri * barW).toInt()
            ctx.fill(briThumbX - 2, briY - 2, briThumbX + 2, briY + briH + 2, 0xFFFFFFFF.toInt())
        }
    }

    private fun renderString(ctx: GuiGraphics, s: Setting.StringSetting, x: Int, y: Int, width: Int) {
        ctx.fill(x, y, x + width, y + 26, 0x10000000.toInt())
        GUIFontRenderer.draw(ctx, s.name, (x + 4).toFloat(), (y + 4).toFloat(), VoltHackTheme.textSecondary)
        val display = if (s.value.length > 20) s.value.take(17) + "..." else s.value
        GUIFontRenderer.draw(ctx, display, (x + 4).toFloat(), (y + 14).toFloat(), VoltHackTheme.accent)
    }

    private fun clickBoolean(mx: Int, my: Int, s: Setting.Boolean, x: Int, y: Int, width: Int): Boolean {
        if (my in y..y + 26 && mx in x..x + width) {
            s.value = !s.value
            return true
        }
        return false
    }

    private fun clickFloat(mx: Int, my: Int, s: Setting.Float, x: Int, y: Int, width: Int): Boolean {
        val slY = y + 20
        if (my in slY - 3..slY + 7) {
            val ratio = ((mx - x - 2).toFloat() / (width - 4)).coerceIn(0f, 1f)
            val range = s.max - s.min
            val steps = (range / s.step).roundToInt().coerceAtLeast(1)
            val stepped = (ratio * steps).roundToInt().coerceIn(0, steps)
            s.value = s.min + stepped * s.step
            dragging = Dragging(s, mx, s.value, x + 2, width - 4)
            return true
        }
        return false
    }

    private fun clickInt(mx: Int, my: Int, s: Setting.Int, x: Int, y: Int, width: Int): Boolean {
        val slY = y + 20
        if (my in slY - 3..slY + 7) {
            val ratio = ((mx - x - 2).toFloat() / (width - 4)).coerceIn(0f, 1f)
            val range = s.max - s.min
            s.value = (s.min + (ratio * range).roundToInt()).coerceIn(s.min, s.max)
            dragging = Dragging(s, mx, (s.value - s.min).toFloat(), x + 2, width - 4)
            return true
        }
        return false
    }

    private fun clickMode(mx: Int, my: Int, s: Setting.Mode, x: Int, y: Int, width: Int, button: Int): Boolean {
        if (my in y..y + 26) {
            if (button == 1) {
                expandedModeSetting = if (expandedModeSetting == s) null else s
            } else {
                val idx = s.modes.indexOf(s.value).coerceAtLeast(0)
                s.value = s.modes[(idx + 1) % s.modes.size]
            }
            return true
        }
        
        if (expandedModeSetting == s && my in y + 26..y + 26 + s.modes.size * 18) {
            val clickedIdx = (my - y - 26) / 18
            if (clickedIdx in s.modes.indices) {
                s.value = s.modes[clickedIdx]
                return true
            }
        }
        
        return false
    }

    private fun clickColor(mx: Int, my: Int, s: Setting.Color, x: Int, y: Int, width: Int): Boolean {
        if (my in y..y + 26) {
            expandedColorSetting = if (expandedColorSetting == s) null else s
            return true
        }

        if (expandedColorSetting == s) {
            val barX = x + 6
            val barW = width - 12
            
            val presetY = y + 32
            if (my in presetY..presetY + 14) {
                for (i in PRESET_COLORS.indices) {
                    val pX = x + 6 + i * 20
                    if (mx in pX..pX + 14) {
                        s.value = PRESET_COLORS[i]
                        return true
                    }
                }
            }

            val barY = y + 54
            if (my in barY - 2..barY + 10) {
                val ratio = ((mx - barX).toFloat() / barW).coerceIn(0f, 1f)
                val hsb = FloatArray(3)
                val r = (s.value shr 16) and 0xFF
                val g = (s.value shr 8) and 0xFF
                val b = s.value and 0xFF
                java.awt.Color.RGBtoHSB(r, g, b, hsb)
                s.value = java.awt.Color.HSBtoRGB(ratio, hsb[1], hsb[2])
                dragging = Dragging(s, mx, ratio, barX, barW, "color_hue")
                return true
            }

            val briY = y + 72
            if (my in briY - 2..briY + 10) {
                val ratio = ((mx - barX).toFloat() / barW).coerceIn(0f, 1f)
                val hsb = FloatArray(3)
                val r = (s.value shr 16) and 0xFF
                val g = (s.value shr 8) and 0xFF
                val b = s.value and 0xFF
                java.awt.Color.RGBtoHSB(r, g, b, hsb)
                s.value = java.awt.Color.HSBtoRGB(hsb[0], hsb[1], ratio)
                dragging = Dragging(s, mx, ratio, barX, barW, "color_bri")
                return true
            }
        }
        return false
    }
}
