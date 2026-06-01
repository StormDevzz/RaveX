package ravex.gui.clickgui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;
import org.lwjgl.glfw.GLFW;
import ravex.modules.Category;
import ravex.utility.render.FontRenderUtility;

import java.util.ArrayList;
import java.util.List;

public class ClickGUI extends Screen {
    public static ModuleButton bindingModuleButton = null;
    public static String hoveredDescription = null;
    public static String searchQuery = "";
    public static ravex.parameter.ColorParameter activeColorParameter = null;
    public static ColorPaletteModal activeColorPalette = null;
    public static ParameterElement activeStringParameterElement = null;

    private final List<CategoryPanel> panels = new ArrayList<>();
    private final long initTime;
    private double smoothedMaxH = 200.0;

    private boolean closing = false;
    private long closingStartTime = 0;
    private float tooltipAlpha = 0.0f;
    private String activeTooltipText = "";

    private boolean searchFocused = false;
    private float searchAnimProgress = 0f;
    private long searchLastUpdate = System.currentTimeMillis();
    private String searchBeforeEdit = "";
    private int searchCursorCounter = 0;

    private boolean macrosHovered;
    private boolean profilesHovered;
    private boolean configsHovered;

    public ClickGUI() {
        super(Component.literal("RaveX ClickGUI"));
        this.initTime = System.currentTimeMillis();
        int startX = 20;
        int startY = 55;
        int spacing = 15;
        Category[] categories = Category.values();
        for (int i = 0; i < categories.length; i++) {
            panels.add(new CategoryPanel(categories[i], startX + i * (120 + spacing), startY));
        }
        ravex.utility.sound.SoundUtility.playGuiOpen();
    }

    @Override
    protected void init() {
        float spacing = 2;
        float panelW = 120;
        int numPanels = panels.size();
        float totalW = numPanels * panelW + (numPanels - 1) * spacing;
        float startX = (this.width - totalW) / 2;
        int maxH = getMaxPanelHeight();
        float startY = (this.height - maxH) / 2;
        if (startY < 30) startY = 30;

        for (int i = 0; i < numPanels; i++) {
            CategoryPanel panel = panels.get(i);
            panel.setX((int)(startX + i * (panelW + spacing)));
            panel.setY((int)startY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int getMaxPanelHeight() {
        int maxH = 150;
        for (CategoryPanel panel : panels) {
            int h = panel.getCurrentHeight(searchQuery);
            if (h > maxH) maxH = h;
        }
        return maxH;
    }

    private float getResponsiveScale() {
        int numPanels = panels.size();
        if (numPanels == 0) return 1.0f;

        float panelW = 120f;
        float spacing = 2f;
        float totalW = numPanels * panelW + (numPanels - 1) * spacing;
        float totalH = (float) smoothedMaxH + 40f;

        float marginX = 20f;
        float marginY = 30f;
        float availW = this.width - marginX * 2;
        float availH = this.height - marginY * 2;

        if (availW <= 0 || availH <= 0) return 0.5f;

        float scaleW = availW / totalW;
        float scaleH = availH / totalH;
        float scale = Math.min(scaleW, scaleH);

        return Math.max(0.4f, Math.min(1.0f, scale));
    }

    private float getAdaptiveScale() {
        long elapsed = System.currentTimeMillis() - initTime;
        float animProgress = Math.min(1.0f, elapsed / 180.0f);
        float animScale = animProgress * (2.0f - animProgress);

        if (closing) {
            long closingElapsed = System.currentTimeMillis() - closingStartTime;
            if (closingElapsed < 130) {
                float closingProgress = Math.max(0.0f, 1.0f - (closingElapsed / 130.0f));
                animScale = closingProgress * (2.0f - closingProgress);
            }
        }

        float responsiveScale = getResponsiveScale();
        return animScale * responsiveScale;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        FontRenderUtility.setCustomEnabled(false);
        int targetMaxH = getMaxPanelHeight();
        smoothedMaxH += (targetMaxH - smoothedMaxH) * 0.15;

        if (ravex.modules.render.ClickGui.INSTANCE.drawBackground.getValue()) {
            graphics.fillGradient(0, 0, this.width, this.height, ColorUtility.BACKGROUND_START, ColorUtility.BACKGROUND_END);
        }

        String tips = "Esc/Shift — close  •  MMB — bind  •  RMB — settings";
        int tipsW = FontRenderUtility.getStringWidth(tips);
        int tipsX = (this.width - tipsW) / 2;
        int tipsY = this.height - 18;

        int tipBg = ColorUtility.withAlpha(ColorUtility.getActiveColor(), 15);
        graphics.fillGradient(tipsX - 14, tipsY - 5, tipsX + tipsW + 14, tipsY + 12, tipBg, 0x22050508);
        graphics.fill(tipsX - 14, tipsY + 7, tipsX + tipsW + 14, tipsY + 8, ColorUtility.withAlpha(ColorUtility.getActiveColor(), 40));
        FontRenderUtility.drawString(graphics, tips, tipsX, tipsY - 1, 0xFF858599, true);

        float btnScale = Math.max(0.7f, getResponsiveScale());
        int mgW = (int)(52 * btnScale);
        int mgH = (int)(16 * btnScale);
        int mgX = Math.max(4, (int)(10 * btnScale));
        int mgY = this.height - (int)(40 * btnScale);
        macrosHovered = mouseX >= mgX && mouseX <= mgX + mgW && mouseY >= mgY && mouseY <= mgY + mgH;
        profilesHovered = mouseX >= mgX + mgW + 4 && mouseX <= mgX + mgW * 2 + 4 && mouseY >= mgY && mouseY <= mgY + mgH;
        configsHovered = mouseX >= mgX + mgW * 2 + 8 && mouseX <= mgX + mgW * 3 + 8 && mouseY >= mgY && mouseY <= mgY + mgH;
        int activeColor = ColorUtility.getActiveColor();
        int macroBg = macrosHovered ? activeColor : 0xAA0E0E1C;
        int profileBg = profilesHovered ? activeColor : 0xAA0E0E1C;
        int configsBg = configsHovered ? activeColor : 0xAA0E0E1C;

        for (int s = 2; s > 0; s--) {
            int sa = (int)(15 * (1f - s/2f));
            graphics.fill(mgX + s, mgY + s, mgX + mgW - s, mgY + mgH + s, (sa << 24));
            graphics.fill(mgX + mgW + 4 + s, mgY + s, mgX + mgW * 2 + 4 - s, mgY + mgH + s, (sa << 24));
            graphics.fill(mgX + mgW * 2 + 8 + s, mgY + s, mgX + mgW * 3 + 8 - s, mgY + mgH + s, (sa << 24));
        }
        graphics.fillGradient(mgX, mgY, mgX + mgW, mgY + mgH, macroBg, ColorUtility.darker(macroBg, 0.7f));
        graphics.fillGradient(mgX + mgW + 4, mgY, mgX + mgW * 2 + 4, mgY + mgH, profileBg, ColorUtility.darker(profileBg, 0.7f));
        graphics.fillGradient(mgX + mgW * 2 + 8, mgY, mgX + mgW * 3 + 8, mgY + mgH, configsBg, ColorUtility.darker(configsBg, 0.7f));
        graphics.fill(mgX + mgW + 4, mgY + 1, mgX + mgW + 5, mgY + mgH - 1, ColorUtility.withAlpha(activeColor, 80));
        graphics.fill(mgX + mgW * 2 + 8, mgY + 1, mgX + mgW * 2 + 9, mgY + mgH - 1, ColorUtility.withAlpha(activeColor, 80));
        if (macrosHovered) {
            graphics.fill(mgX, mgY + mgH - 1, mgX + mgW, mgY + mgH, activeColor);
        }
        if (profilesHovered) {
            graphics.fill(mgX + mgW + 4, mgY + mgH - 1, mgX + mgW * 2 + 4, mgY + mgH, activeColor);
        }
        if (configsHovered) {
            graphics.fill(mgX + mgW * 2 + 8, mgY + mgH - 1, mgX + mgW * 3 + 8, mgY + mgH, activeColor);
        }

        int textY = mgY + (mgH - 8) / 2;
        FontRenderUtility.drawString(graphics, "Macros", mgX + 8, textY, 0xFFD0D0E0, true);
        FontRenderUtility.drawString(graphics, "Profiles", mgX + mgW + 12, textY, 0xFFD0D0E0, true);
        FontRenderUtility.drawString(graphics, "Configs", mgX + mgW * 2 + 14, textY, 0xFFD0D0E0, true);

        hoveredDescription = null;

        float finalScale = getAdaptiveScale();
        if (closing && (System.currentTimeMillis() - closingStartTime >= 130)) {
            this.minecraft.setScreen(null);
            return;
        }

        float cx = this.width / 2.0f;
        float cy = this.height / 2.0f;

        int mx = (int) ((mouseX - cx) / finalScale + cx);
        int my = (int) ((mouseY - cy) / finalScale + cy);

        // Render search bar in unscaled space so it's always accessible
        renderSearchBar(graphics, mouseX, mouseY);

        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(cx, cy);
        pose.scale(finalScale, finalScale);
        pose.translate(-cx, -cy);

        for (CategoryPanel panel : panels) {
            panel.render(graphics, mx, my, searchQuery);
        }

        pose.popMatrix();

        if (hoveredDescription != null) {
            activeTooltipText = hoveredDescription;
            tooltipAlpha = Math.min(1.0f, tooltipAlpha + partialTicks * 0.15f);
        } else {
            tooltipAlpha = Math.max(0.0f, tooltipAlpha - partialTicks * 0.25f);
        }

        if (tooltipAlpha > 0.02f && !activeTooltipText.isEmpty()) {
            int tx = mouseX + 12;
            int ty = mouseY + 12;
            int tw = FontRenderUtility.getStringWidth(activeTooltipText) + 8;
            int th = 16;

            if (tx + tw > this.width) tx = mouseX - tw - 4;
            if (ty + th > this.height) ty = mouseY - th - 4;

            int alphaInt = Math.round(tooltipAlpha * 230);
            int bg = (alphaInt << 24) | 0x07070B;
            int border = (Math.round(tooltipAlpha * 255) << 24) | (ColorUtility.getActiveColor() & 0xFFFFFF);
            int textCol = (Math.round(tooltipAlpha * 255) << 24) | 0xE0E0E0;

            graphics.fill(tx, ty, tx + tw, ty + th, bg);
            graphics.fill(tx, ty, tx + tw, ty + 1, border);
            FontRenderUtility.drawString(graphics, activeTooltipText, tx + 4, ty + 4, textCol, true);
        }

        if (activeColorPalette != null) {
            activeColorPalette.render(graphics, mouseX, mouseY, this.width, this.height);
        }

        super.render(graphics, mouseX, mouseY, partialTicks);

        FontRenderUtility.setCustomEnabled(false);
    }

    private void renderSearchBar(GuiGraphics graphics, int mouseX, int mouseY) {
        int barW = Math.min(280, this.width - 60);
        int barX = (this.width - barW) / 2;
        int barY = 14;
        int barH = 26;

        long now = System.currentTimeMillis();
        long delta = now - searchLastUpdate;
        searchLastUpdate = now;
        if (delta > 100) delta = 16;

        if (searchFocused) {
            searchAnimProgress = Math.min(1.0f, searchAnimProgress + delta * 0.012f);
            searchCursorCounter++;
        } else {
            searchAnimProgress = Math.max(0.0f, searchAnimProgress - delta * 0.018f);
        }

        int glowColor = ColorUtility.getActiveColor();
        float eased = searchAnimProgress * searchAnimProgress * (3f - 2f * searchAnimProgress);
        int expandedW = barW + (int)(60 * eased);
        int actualX = barX - (expandedW - barW) / 2;

        for (int s = 3; s > 0; s--) {
            int sa = (int)(15 * (1f - s/3f));
            graphics.fill(actualX + s, barY + s, actualX + expandedW - s, barY + barH + s, (sa << 24));
        }
        graphics.fillGradient(actualX, barY, actualX + expandedW, barY + barH, 0xFF0B0B1A, 0xFF14142E);
        graphics.fill(actualX, barY + barH - 2, actualX + expandedW, barY + barH - 1, ColorUtility.withAlpha(glowColor, 40 + (int)(60 * eased)));
        graphics.fill(actualX, barY + barH - 1, actualX + expandedW, barY + barH, glowColor);

        int searchIconSize = 10;
        int iconX = actualX + 10;
        int iconY = barY + (barH - searchIconSize) / 2 + 1;
        for (int dy = 0; dy < searchIconSize; dy++) {
            float dist = Math.abs(dy - searchIconSize / 2f) / (searchIconSize / 2f);
            int dx = (int)(4 * dist);
            graphics.fill(iconX + dx, iconY + dy, iconX + searchIconSize - dx, iconY + dy + 1, ColorUtility.withAlpha(glowColor, 80 + (int)(40 * eased)));
        }

        String searchText = searchQuery;
        if (searchText.isEmpty() && !searchFocused) {
            FontRenderUtility.drawString(graphics, "Search modules\u2026", actualX + 24, barY + 9, 0xFF505068, true);
        } else {
            FontRenderUtility.drawString(graphics, searchText, actualX + 24, barY + 9, 0xFFC0C0D0, true);
            if (searchFocused) {
                int textW = FontRenderUtility.getStringWidth(searchText);
                float cursorBlink = (float)Math.sin(searchCursorCounter * 0.1f);
                int cursorAlpha = (int)(150 + 105 * cursorBlink * cursorBlink * cursorBlink);
                graphics.fill(actualX + 24 + textW, barY + 8, actualX + 26 + textW, barY + 19, (cursorAlpha << 24) | (glowColor & 0xFFFFFF));
            }
        }

        graphics.fill(actualX + 22, barY + 7, actualX + 24, barY + 19, ColorUtility.withAlpha(glowColor, 80));

        int resultCount = 0;
        if (!searchQuery.isEmpty()) {
            for (var panel : panels) {
                resultCount += panel.getMatchCount(searchQuery);
            }
            if (resultCount > 0) {
                String countText = resultCount + (resultCount == 1 ? " result" : " results");
                int cw = FontRenderUtility.getStringWidth(countText);
                FontRenderUtility.drawString(graphics, countText, actualX + expandedW - cw - 10, barY + 9, 0xFF606080, true);
                graphics.fill(actualX + expandedW - cw - 14, barY + 8, actualX + expandedW - cw - 12, barY + 19, ColorUtility.withAlpha(glowColor, 60));
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        if (activeColorPalette != null) {
            return activeColorPalette.mouseClicked(event.x(), event.y(), event.button());
        }

        float finalScale = getAdaptiveScale();
        float cx = this.width / 2.0f;
        float cy = this.height / 2.0f;

        double mx = (event.x() - cx) / finalScale + cx;
        double my = (event.y() - cy) / finalScale + cy;

        int barW = Math.min(240, this.width - 60);
        int barX = (this.width - barW) / 2;
        int barY = 12;
        int barH = 22;

        if (event.x() >= barX && event.x() <= barX + barW && event.y() >= barY && event.y() <= barY + barH) {
            searchFocused = true;
            return true;
        }

        if (bindingModuleButton != null) {
            return super.mouseClicked(event, handled);
        }

        float btnScale = Math.max(0.7f, getResponsiveScale());
        int mgW = (int)(52 * btnScale);
        int mgH = (int)(16 * btnScale);
        int mgX = Math.max(4, (int)(10 * btnScale));
        int mgY = this.height - (int)(40 * btnScale);

        if (event.x() >= mgX && event.x() <= mgX + mgW && event.y() >= mgY && event.y() <= mgY + mgH) {
            this.minecraft.setScreen(new MacroScreen(this));
            return true;
        }
        if (event.x() >= mgX + mgW + 4 && event.x() <= mgX + mgW * 2 + 4 && event.y() >= mgY && event.y() <= mgY + mgH) {
            this.minecraft.setScreen(new ProfilesScreen(this));
            return true;
        }
        if (event.x() >= mgX + mgW * 2 + 8 && event.x() <= mgX + mgW * 3 + 8 && event.y() >= mgY && event.y() <= mgY + mgH) {
            this.minecraft.setScreen(new ConfigsScreen(this));
            return true;
        }

        for (CategoryPanel panel : panels) {
            if (panel.mouseClicked(mx, my, event.button(), this.minecraft)) {
                return true;
            }
        }
        return super.mouseClicked(event, handled);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (activeColorPalette != null) {
            return activeColorPalette.keyPressed(event.key());
        }

        int key = event.key();

        if (bindingModuleButton != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                bindingModuleButton.getModule().setKeyBind(GLFW.GLFW_KEY_UNKNOWN);
            } else {
                bindingModuleButton.getModule().setKeyBind(key);
            }
            bindingModuleButton = null;
            if (this.minecraft.player != null) {
                this.minecraft.player.playSound(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),
                    0.5f, 1.5f
                );
            }
            return true;
        }

        if (activeStringParameterElement != null) {
            ravex.parameter.StringParameter sp = (ravex.parameter.StringParameter) activeStringParameterElement.getParameter();
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER) {
                activeStringParameterElement = null;
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                String val = sp.getValue();
                if (!val.isEmpty()) {
                    sp.setValue(val.substring(0, val.length() - 1));
                }
                return true;
            }
            return true;
        }

        if (searchFocused) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                searchFocused = false;
                searchQuery = "";
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
                return true;
            }
            return true;
        }

        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (activeColorPalette != null) {
            return true;
        }
        if (activeStringParameterElement != null) {
            ravex.parameter.StringParameter sp = (ravex.parameter.StringParameter) activeStringParameterElement.getParameter();
            String text = event.codepointAsString();
            if (!text.isEmpty() && text.charAt(0) >= 32 && text.charAt(0) < 127) {
                sp.setValue(sp.getValue() + text);
            }
            return true;
        }
        if (searchFocused) {
            String text = event.codepointAsString();
            if (!text.isEmpty() && text.charAt(0) >= 32 && text.charAt(0) < 127) {
                searchQuery += text;
            }
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public void onClose() {
        activeStringParameterElement = null;
        if (!closing) {
            closing = true;
            closingStartTime = System.currentTimeMillis();
            ravex.utility.sound.SoundUtility.playGuiClose();
            ravex.manager.ConfigManager.INSTANCE.save("default");
        }
        FontRenderUtility.setCustomEnabled(false);
    }
}
