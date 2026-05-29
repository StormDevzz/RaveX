package ravex.gui.clickgui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;
import org.lwjgl.glfw.GLFW;
import ravex.modules.Category;

import java.util.ArrayList;
import java.util.List;

public class ClickGUI extends Screen {
    public static ModuleButton bindingModuleButton = null;
    public static String hoveredDescription = null;
    public static String searchQuery = "";
    public static ravex.parameter.ColorParameter activeColorParameter = null;
    public static ColorPaletteModal activeColorPalette = null;

    private final List<CategoryPanel> panels = new ArrayList<>();
    private final long initTime;

    private boolean closing = false;
    private long closingStartTime = 0;
    private float tooltipAlpha = 0.0f;
    private String activeTooltipText = "";

    private boolean searchFocused = false;
    private float searchAnimProgress = 0f;
    private long searchLastUpdate = System.currentTimeMillis();
    private String searchBeforeEdit = "";
    private int searchCursorCounter = 0;

    public ClickGUI() {
        super(Component.literal("RaveX ClickGUI"));
        this.initTime = System.currentTimeMillis();
        int startX = 20;
        int startY = 55;
        int spacing = 15;
        Category[] categories = Category.values();
        for (int i = 0; i < categories.length; i++) {
            panels.add(new CategoryPanel(categories[i], startX + i * (100 + spacing), startY));
        }
        ravex.utility.sound.SoundUtility.playGuiOpen();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (ravex.modules.render.ClickGui.INSTANCE.drawBackground.getValue()) {
            graphics.fillGradient(0, 0, this.width, this.height, ColorUtility.BACKGROUND_START, ColorUtility.BACKGROUND_END);
        }

        String tips = "Right Shift / Esc to Close  •  Middle Click to Bind  •  Right Click to Configure Settings";
        int tipsW = this.font.width(tips);
        int tipsX = (this.width - tipsW) / 2;
        int tipsY = this.height - 20;

        graphics.fill(tipsX - 10, tipsY - 5, tipsX + tipsW + 10, tipsY + 13, 0x88050508);
        graphics.drawString(this.font, tips, tipsX, tipsY, 0xFF858599, false);

        hoveredDescription = null;

        long elapsed = System.currentTimeMillis() - initTime;
        float animProgress = Math.min(1.0f, elapsed / 180.0f);
        float scale = animProgress * (2.0f - animProgress);

        if (closing) {
            long closingElapsed = System.currentTimeMillis() - closingStartTime;
            if (closingElapsed >= 130) {
                this.minecraft.setScreen(null);
                return;
            }
            float closingProgress = Math.max(0.0f, 1.0f - (closingElapsed / 130.0f));
            scale = closingProgress * (2.0f - closingProgress);
        }

        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(this.width / 2.0f, this.height / 2.0f);
        pose.scale(scale, scale);
        pose.translate(-this.width / 2.0f, -this.height / 2.0f);

        renderSearchBar(graphics, mouseX, mouseY);

        for (CategoryPanel panel : panels) {
            panel.render(graphics, this.font, mouseX, mouseY, searchQuery);
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
            int tw = this.font.width(activeTooltipText) + 8;
            int th = 16;

            if (tx + tw > this.width) tx = mouseX - tw - 4;
            if (ty + th > this.height) ty = mouseY - th - 4;

            int alphaInt = Math.round(tooltipAlpha * 230);
            int bg = (alphaInt << 24) | 0x07070B;
            int border = (Math.round(tooltipAlpha * 255) << 24) | (ColorUtility.getActiveColor() & 0xFFFFFF);
            int textCol = (Math.round(tooltipAlpha * 255) << 24) | 0xE0E0E0;

            graphics.fill(tx, ty, tx + tw, ty + th, bg);
            graphics.fill(tx, ty, tx + tw, ty + 1, border);
            graphics.drawString(this.font, activeTooltipText, tx + 4, ty + 4, textCol, false);
        }

        if (activeColorPalette != null) {
            activeColorPalette.render(graphics, this.font, mouseX, mouseY, this.width, this.height);
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    private void renderSearchBar(GuiGraphics graphics, int mouseX, int mouseY) {
        int barX = this.width / 2 - 120;
        int barY = 12;
        int barW = 240;
        int barH = 22;

        long now = System.currentTimeMillis();
        long delta = now - searchLastUpdate;
        searchLastUpdate = now;
        if (delta > 100) delta = 16;

        if (searchFocused) {
            searchAnimProgress = Math.min(1.0f, searchAnimProgress + delta * 0.01f);
            searchCursorCounter++;
        } else {
            searchAnimProgress = Math.max(0.0f, searchAnimProgress - delta * 0.015f);
        }

        int glowColor = ColorUtility.getActiveColor();
        int baseColor = 0xFF0D0D1A;

        int animWidth = (int)(barW * (0.5f + 0.5f * searchAnimProgress)) + (int)(barW * 0.5f * (1f - searchAnimProgress));
        int expandedW = Math.max(barW, animWidth);
        int actualX = barX - (expandedW - barW) / 2;

        graphics.fill(actualX, barY, actualX + expandedW, barY + barH, 0xFF0A0A14);
        graphics.fill(actualX, barY + barH - 1, actualX + expandedW, barY + barH, glowColor);

        String searchText = searchQuery;
        if (searchText.isEmpty() && !searchFocused) {
            graphics.drawString(this.font, "\uD83D\uDD0D Search modules...", actualX + 8, barY + 7, 0xFF505060, false);
        } else {
            graphics.drawString(this.font, "\uD83D\uDD0D " + searchText, actualX + 8, barY + 7, 0xFFC0C0D0, false);
            if (searchFocused && (searchCursorCounter / 20) % 2 == 0) {
                int textW = this.font.width(searchText);
                graphics.fill(actualX + 18 + textW, barY + 6, actualX + 20 + textW, barY + 16, 0xFFC0C0D0);
            }
        }

        int resultCount = 0;
        if (!searchQuery.isEmpty()) {
            for (var panel : panels) {
                resultCount += panel.getMatchCount(searchQuery);
            }
            if (resultCount > 0) {
                String countText = resultCount + " found";
                int cw = this.font.width(countText);
                graphics.drawString(this.font, countText, actualX + expandedW - cw - 8, barY + 7, 0xFF707080, false);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        if (activeColorPalette != null) {
            return activeColorPalette.mouseClicked(event.x(), event.y(), event.button());
        }

        int barX = this.width / 2 - 120;
        int barY = 12;
        int barW = 240;
        int barH = 22;

        if (event.x() >= barX && event.x() <= barX + barW && event.y() >= barY && event.y() <= barY + barH) {
            searchFocused = true;
            return true;
        }

        if (bindingModuleButton != null) {
            return super.mouseClicked(event, handled);
        }

        for (CategoryPanel panel : panels) {
            if (panel.mouseClicked(event.x(), event.y(), event.button(), this.minecraft)) {
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
        if (!closing) {
            closing = true;
            closingStartTime = System.currentTimeMillis();
            ravex.utility.sound.SoundUtility.playGuiClose();
            ravex.manager.ConfigManager.INSTANCE.save("default");
        }
    }
}
