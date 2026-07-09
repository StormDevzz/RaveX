package ravex.gui.clickgui.components;

import net.minecraft.client.gui.GuiGraphics;
import ravex.utility.render.FontRenderUtility;
import ravex.gui.clickgui.ColorUtility;

public class TabButtonWidget {
    private final String label;
    private final int width;
    private final int height;

    public TabButtonWidget(String label, int width, int height) {
        this.label = label;
        this.width = width;
        this.height = height;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, boolean active, int activeColor) {
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        int bg = active ? ColorUtility.withAlpha(activeColor, 35) : (hovered ? 0x221A1A2E : 0x0D0A0A10);
        graphics.fill(x, y, x + width, y + height, bg);

        int outline = active ? activeColor : (hovered ? ColorUtility.withAlpha(activeColor, 100) : 0x1A858599);
        graphics.fill(x, y, x + width, y + 1, outline);
        graphics.fill(x, y + height - 1, x + width, y + height, outline);
        graphics.fill(x, y, x + 1, y + height, outline);
        graphics.fill(x + width - 1, y, x + width, y + height, outline);

        int textW = FontRenderUtility.getStringWidth(label);
        FontRenderUtility.drawString(graphics, label, x + (width - textW) / 2, y + (height - 10) / 2, active ? 0xFFFFFFFF : 0xFF858599, true);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
