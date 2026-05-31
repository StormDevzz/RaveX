package ravex.utility.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class CustomFontRenderer {
    private static CustomFontRenderer INSTANCE;

    public CustomFontRenderer() {
    }

    public static CustomFontRenderer getInstance() {
        if (INSTANCE == null) INSTANCE = new CustomFontRenderer();
        return INSTANCE;
    }

    public int drawString(GuiGraphics graphics, String text, int x, int y, int color, boolean shadow) {
        Font font = Minecraft.getInstance().font;
        if (font == null || text == null || text.isEmpty()) return x;
        graphics.drawString(font, text, x, y, color, shadow);
        return x + font.width(text);
    }

    public int width(String text) {
        Font font = Minecraft.getInstance().font;
        if (font == null || text == null) return 0;
        return font.width(text);
    }

    public int getHeight() {
        Font font = Minecraft.getInstance().font;
        if (font == null) return 12;
        return font.lineHeight;
    }

    public boolean isReady() { return true; }
    public String getFallbackMessage() { return ""; }
}
