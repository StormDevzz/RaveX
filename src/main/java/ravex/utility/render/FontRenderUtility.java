package ravex.utility.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class FontRenderUtility {
    public static void drawString(GuiGraphics graphics, String text, int x, int y, int color, boolean shadow) {
        Font font = Minecraft.getInstance().font;
        graphics.drawString(font, text, x, y, color, shadow);
    }

    public static int getStringWidth(String text) {
        return Minecraft.getInstance().font.width(text);
    }
}
