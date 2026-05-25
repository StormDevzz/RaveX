package volthack.hud.elements

import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.Identifier
import volthack.VoltHack
import volthack.gui.font.GUIFontRenderer
import volthack.gui.theme.VoltHackTheme
import volthack.hud.HUDElement
import volthack.util.discord.DiscordRPC
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URL
import java.util.function.Supplier
import javax.imageio.ImageIO
import kotlin.concurrent.thread

class WatermarkElement : HUDElement("Watermark") {
    private val showDiscord by boolean("Show Discord", true)
    private val customColor by color("Color", 0xFF6C63FF.toInt())

    private val text = "VoltHack v${VoltHack.version}"

    private var avatarTexture: Identifier? = null
    private var lastAvatarUrl: String? = null
    private var downloading = false

    init {
        x = 4
        y = 4
        enabled = false
    }

    private fun loadAvatar(url: String) {
        if (downloading) return
        downloading = true
        thread {
            try {
                val conn = java.net.URI.create(url).toURL().openConnection()
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                val isStream = conn.getInputStream()
                val image = ImageIO.read(isStream)
                if (image != null) {
                    val baos = ByteArrayOutputStream()
                    ImageIO.write(image, "png", baos)
                    val bytes = baos.toByteArray()
                    val nativeImage = NativeImage.read(ByteArrayInputStream(bytes))

                    val mc = Minecraft.getInstance()
                    mc.execute {
                        val tex = DynamicTexture(Supplier { "volthack_discord_avatar" }, nativeImage)
                        val id = Identifier.tryParse("volthack:discord_avatar")
                        if (id != null) {
                            mc.textureManager.register(id, tex)
                            avatarTexture = id
                        }
                        lastAvatarUrl = url
                        downloading = false
                    }
                } else {
                    downloading = false
                }
            } catch (e: Exception) {
                downloading = false
            }
        }
    }

    override fun draw(ctx: GuiGraphics) {
        val showDC = showDiscord && DiscordRPC.isConnected
        val dcName = DiscordRPC.discordUsername
        val dcAvatar = DiscordRPC.discordAvatarUrl

        if (showDC && dcAvatar != null && dcAvatar != lastAvatarUrl) {
            loadAvatar(dcAvatar)
        }

        val baseW = GUIFontRenderer.width(text)
        val texVal = avatarTexture
        val hasAvatar = showDC && texVal != null
        val dcTextWidth = if (showDC && dcName != null) GUIFontRenderer.width(" | $dcName") else 0
        val avatarW = if (hasAvatar) 18 else 0

        cachedWidth = baseW + dcTextWidth + avatarW + 16 + (if (hasAvatar) 6 else 0)
        cachedHeight = GUIFontRenderer.height + 8

        // Draw card background
        ctx.fill(x, y, x + cachedWidth, y + cachedHeight, VoltHackTheme.surface)
        ctx.fill(x, y, x + cachedWidth, y + 1, customColor)

        var cx = x + 8
        if (hasAvatar) {
            // 9-argument blit: Identifier, x, y, u, v, width, height, textureWidth, textureHeight
            ctx.blit(
                texVal,
                cx,
                y + (cachedHeight - 14) / 2,
                0,
                0,
                14f,
                14f,
                14f,
                14f
            )
            cx += 18
        }

        // Draw VoltHack text
        GUIFontRenderer.draw(
            ctx,
            text,
            cx.toFloat(),
            (y + (cachedHeight - GUIFontRenderer.height) / 2f),
            customColor
        )
        cx += baseW

        // Draw Discord username
        if (showDC && dcName != null) {
            GUIFontRenderer.draw(
                ctx,
                " | $dcName",
                cx.toFloat(),
                (y + (cachedHeight - GUIFontRenderer.height) / 2f),
                VoltHackTheme.textSecondary
            )
        }
    }
}
