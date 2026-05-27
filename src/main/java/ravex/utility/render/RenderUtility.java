package ravex.utility.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;

public class RenderUtility {
    public static void drawRect(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + height, color);
    }

    public static void drawGradientRect(GuiGraphics graphics, int x, int y, int width, int height, int startColor, int endColor) {
        graphics.fillGradient(x, y, x + width, y + height, startColor, endColor);
    }

    public static int getRainbowColor(float speed, float saturation, float brightness) {
        float hue = (System.currentTimeMillis() % (int)(3000 / speed)) / (3000 / speed);
        return java.awt.Color.HSBtoRGB(hue, saturation, brightness);
    }
}
