package volthack.hud.elements

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import volthack.gui.theme.VoltHackTheme
import volthack.hud.HUDElement
import java.text.SimpleDateFormat
import java.util.Date
import java.util.LinkedList

class ChatElement : HUDElement("Chat") {
    private val maxMessages by int("Max Messages", 100, 10, 500)
    private val chatWidth by float("Chat Width", 320f, 100f, 600f, 10f)
    private val chatHeight by float("Chat Height", 180f, 60f, 400f, 10f)
    private val backgroundOpacity by float("Background Opacity", 0.5f, 0f, 1f, 0.1f)
    private val showTimestamps by boolean("Show Timestamps", false)

    private val messages = LinkedList<ChatMessage>()
    private var scrollPos = 0

    init {
        x = 10
        y = 300
        cachedWidth = 320
        cachedHeight = 180
        enabled = false
    }

    data class ChatMessage(val text: String, val timestamp: Long = System.currentTimeMillis())

    fun addMessage(text: String) {
        messages.addLast(ChatMessage(text))
        while (messages.size > maxMessages) {
            messages.removeFirst()
        }
    }

    fun addMessage(component: Component) {
        addMessage(component.string)
    }

    override fun draw(ctx: GuiGraphics) {
        val mc = Minecraft.getInstance()
        val font = mc.font

        if (messages.isEmpty()) return

        val bgColor = (0x0A0A14 and 0x00FFFFFF) or ((backgroundOpacity * 255).toInt().coerceIn(0, 255) shl 24)
        val displayHeight = chatHeight.toInt().coerceAtMost(400)

        ctx.fill(x, y, x + chatWidth.toInt(), y + displayHeight, bgColor)

        var currentY = y + displayHeight - font.lineHeight - 4
        val startIdx = (messages.size - 1 - scrollPos).coerceIn(0, messages.size)

        for (i in startIdx downTo 0) {
            if (currentY < y) break
            val msg = messages[i]
            val displayText = if (showTimestamps) {
                val time = SimpleDateFormat("HH:mm").format(Date(msg.timestamp))
                "§7[$time§7] §r${msg.text}"
            } else {
                msg.text
            }

            ctx.drawString(font, displayText, x + 4, currentY, VoltHackTheme.textPrimary, true)
            currentY -= font.lineHeight + 2
        }

        cachedWidth = chatWidth.toInt()
        cachedHeight = displayHeight
    }

    fun scroll(amount: Int) {
        scrollPos = (scrollPos + amount).coerceIn(0, messages.size)
    }

    fun clear() {
        messages.clear()
        scrollPos = 0
    }
}
