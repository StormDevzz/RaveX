package ravex.utility.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class FontRenderUtility {
    public static void drawString(GuiGraphics graphics, String text, int x, int y, int color, boolean shadow) {
        if (useCustomFont()) {
            CustomFontRenderer.getInstance().drawString(graphics, text, x, y, color, shadow);
        } else {
            Font font = Minecraft.getInstance().font;
            graphics.drawString(font, text, x, y, color, shadow);
        }
    }

    public static int getStringWidth(String text) {
        if (useCustomFont()) {
            return CustomFontRenderer.getInstance().width(text);
        }
        return Minecraft.getInstance().font.width(text);
    }

    public static int getFontHeight() {
        if (useCustomFont()) {
            return CustomFontRenderer.getInstance().getHeight();
        }
        return Minecraft.getInstance().font.lineHeight;
    }

    private static boolean useCustomFont() {
        return false;
    }
}
