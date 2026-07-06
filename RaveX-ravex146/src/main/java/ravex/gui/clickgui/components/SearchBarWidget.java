package ravex.gui.clickgui.components;

import net.minecraft.client.gui.GuiGraphics;
import ravex.utility.render.FontRenderUtility;
import ravex.gui.clickgui.ColorUtility;

public class SearchBarWidget {
    private String query = "";
    private boolean focused = false;
    private float animProgress = 0f;
    private int cursorCounter = 0;
    private final String placeholder;

    public SearchBarWidget(String placeholder) {
        this.placeholder = placeholder;
    }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public boolean isFocused() { return focused; }
    public void setFocused(boolean focused) { this.focused = focused; }

    public void update(long delta) {
        if (focused) {
            animProgress = Math.min(1.0f, animProgress + delta * 0.015f);
            cursorCounter++;
        } else {
            animProgress = Math.max(0.0f, animProgress - delta * 0.018f);
        }
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int width, int height) {
        float eased = animProgress * animProgress * (3f - 2f * animProgress);
        int expandedW = width + (int)(30 * eased);
        int actualX = x - (expandedW - width) / 2;

        int activeColor = ColorUtility.getActiveColor();

        for (int s = 3; s > 0; s--) {
            int sa = (int)(15 * (1f - s/3f));
            graphics.fill(actualX + s, y + s, actualX + expandedW - s, y + height + s, (sa << 24));
        }
        graphics.fillGradient(actualX, y, actualX + expandedW, y + height, 0xFF0B0B1A, 0xFF14142E);
        graphics.fill(actualX, y + height - 2, actualX + expandedW, y + height - 1,
            ColorUtility.withAlpha(activeColor, 40 + (int)(60 * eased)));
        graphics.fill(actualX, y + height - 1, actualX + expandedW, y + height, activeColor);

        FontRenderUtility.drawString(graphics, "🔍", actualX + 8, y + (height - 10) / 2, ColorUtility.withAlpha(activeColor, 120), true);

        if (query.isEmpty() && !focused) {
            FontRenderUtility.drawString(graphics, placeholder, actualX + 26, y + (height - 10) / 2, 0xFF505068, true);
        } else {
            FontRenderUtility.drawString(graphics, query, actualX + 26, y + (height - 10) / 2, 0xFFC0C0D0, true);
            if (focused) {
                int textW = FontRenderUtility.getStringWidth(query);
                float cursorBlink = (float)Math.sin(cursorCounter * 0.1f);
                int cursorAlpha = (int)(150 + 105 * cursorBlink * cursorBlink * cursorBlink);
                graphics.fill(actualX + 26 + textW, y + (height - 10) / 2, actualX + 28 + textW, y + (height - 10) / 2 + 11,
                    (cursorAlpha << 24) | (activeColor & 0xFFFFFF));
            }
        }
        graphics.fill(actualX + 24, y + 6, actualX + 26, y + height - 6, ColorUtility.withAlpha(activeColor, 80));
    }

    public boolean mouseClicked(double mouseX, double mouseY, int x, int y, int width, int height) {
        float eased = animProgress * animProgress * (3f - 2f * animProgress);
        int expandedW = width + (int)(30 * eased);
        int actualX = x - (expandedW - width) / 2;

        if (mouseX >= actualX && mouseX <= actualX + expandedW && mouseY >= y && mouseY <= y + height) {
            focused = true;
            return true;
        }
        focused = false;
        return false;
    }

    public void charTyped(char codePoint) {
        if (focused && codePoint >= 32 && codePoint < 127) {
            query += codePoint;
        }
    }

    public void keyPressed(int key) {
        if (focused) {
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE && !query.isEmpty()) {
                query = query.substring(0, query.length() - 1);
            }
        }
    }
}
