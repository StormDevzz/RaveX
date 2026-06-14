package ravex.gui.clickgui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import ravex.gui.clickgui.ColorUtility;
import ravex.modules.Category;
import ravex.utility.render.BlurUtility;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClickGUI extends Screen {
    public static ModuleButton bindingModuleButton = null;
    public static String hoveredDescription = null;
    public static String searchQuery = "";
    public static ravex.parameter.ColorParameter activeColorParameter = null;
    public static ColorPaletteModal activeColorPalette = null;
    public static ParameterElement activeStringParameterElement = null;
    public static ParameterElement activeKeybindElement = null;

    public static Identifier getCategoryTexture(Category cat) {
        return ravex.utility.render.TextureLoader.getCategoryTexture(cat);
    }

    public static Identifier getSearchTexture() {
        return ravex.utility.render.TextureLoader.getSearchTexture();
    }

    private final Map<Category, CategoryPanel> panelMap = new HashMap<>();
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

    // ── Star particles ───────────────────────────────────────────────────────────
    private static final int STAR_COUNT = 55;
    private final float[] starX   = new float[STAR_COUNT];
    private final float[] starY   = new float[STAR_COUNT];
    private final float[] starVx  = new float[STAR_COUNT];
    private final float[] starVy  = new float[STAR_COUNT];
    private final float[] starAlpha = new float[STAR_COUNT];
    private final float[] starSize  = new float[STAR_COUNT];
    private boolean starsInit = false;

    private long lastStarTick = 0;

    public ClickGUI() {
        super(Component.literal("RaveX ClickGUI"));
        this.initTime = System.currentTimeMillis();

        if (ravex.modules.render.ClickGui.INSTANCE.blur.getValue()) {
            BlurUtility.enable();
        }

        ravex.utility.render.TextureLoader.preloadAll();

        int spacing = 15;
        int panelW = 120;
        Map<Category, int[]> saved = ravex.manager.LayoutManager.INSTANCE.load();
        Category[] categories = Category.values();
        for (Category cat : categories) {
            int[] pos = saved.get(cat);
            int px, py;
            if (pos != null) {
                px = pos[0];
                py = pos[1];
            } else {
                int i = cat.ordinal();
                px = 20 + i * (panelW + spacing);
                py = 55;
            }
            CategoryPanel p = new CategoryPanel(cat, px, py);
            panels.add(p);
            panelMap.put(cat, p);
        }
        ravex.utility.sound.SoundUtility.playGuiOpen();
    }

    @Override
    protected void init() {
        Map<Category, int[]> saved = ravex.manager.LayoutManager.INSTANCE.load();
        for (CategoryPanel panel : panels) {
            int[] pos = saved.get(panel.getCategory());
            if (pos == null) {
                int spacing = 2;
                int panelW = 120;
                int numPanels = panels.size();
                float totalW = numPanels * panelW + (numPanels - 1) * spacing;
                float startX = (this.width - totalW) / 2;
                int maxH = getMaxPanelHeight();
                float startY = (this.height - maxH) / 2;
                if (startY < 30) startY = 30;
                int i = panel.getCategory().ordinal();
                panel.setX((int)(startX + i * (panelW + spacing)));
                panel.setY((int)startY);
            }
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
        int targetMaxH = getMaxPanelHeight();
        smoothedMaxH += (targetMaxH - smoothedMaxH) * 0.15;

        // ── Background ──────────────────────────────────────────────────────────────
        boolean drawBg = ravex.modules.render.ClickGui.INSTANCE.drawBackground.getValue();
        if (drawBg) {
            graphics.fillGradient(0, 0, this.width, this.height, ColorUtility.BACKGROUND_START, ColorUtility.BACKGROUND_END);
            // Vignette effect
            int edgeColor = ColorUtility.withAlpha(ColorUtility.getActiveColor(), 6);
            graphics.fillGradient(0, 0, this.width, 1, edgeColor, 0x00000000);
        }
        if (ravex.modules.client.GuiParticles.INSTANCE.getEnabled()) {
            renderStars(graphics);
        }

        String tips = "Esc/Shift — close  •  LMB — toggle  •  RMB — settings  •  MMB — bind";
        int tipsW = FontRenderUtility.getStringWidth(tips);
        int tipsX = (this.width - tipsW) / 2;
        int tipsY = this.height - 18;

        int ac = ColorUtility.getActiveColor();
        int tipBg = ColorUtility.withAlpha(ac, 18);
        graphics.fillGradient(tipsX - 14, tipsY - 5, tipsX + tipsW + 14, tipsY + 12, tipBg, ColorUtility.withAlpha(ac, 4));
        graphics.fill(tipsX - 14, tipsY + 7, tipsX + tipsW + 14, tipsY + 8, ColorUtility.withAlpha(ac, 50));
        FontRenderUtility.drawString(graphics, tips, tipsX, tipsY - 1, 0xFF858599, true);

        // ── Bottom nav buttons ───────────────────────────────────────────────────
        //    Macros  |  Profiles  |  Configs   — centered pill buttons
        float btnScale = Math.max(0.65f, getResponsiveScale());
        int mgW = (int)(60 * btnScale);
        int mgH = (int)(20 * btnScale);
        int mgGap = (int)(6 * btnScale);
        int totalBtnW = 3 * mgW + 2 * mgGap;
        int mgX = (this.width - totalBtnW) / 2;
        int mgY = this.height - (int)(38 * btnScale);
        int mgRadius = Math.max(3, (int)(5 * btnScale));

        macrosHovered   = mouseX >= mgX && mouseX <= mgX + mgW && mouseY >= mgY && mouseY <= mgY + mgH;
        profilesHovered = mouseX >= mgX + mgW + mgGap && mouseX <= mgX + 2 * mgW + mgGap && mouseY >= mgY && mouseY <= mgY + mgH;
        configsHovered  = mouseX >= mgX + 2 * (mgW + mgGap) && mouseX <= mgX + 3 * mgW + 2 * mgGap && mouseY >= mgY && mouseY <= mgY + mgH;

        int activeColor = ColorUtility.getActiveColor();

        int[] bxArr   = { mgX, mgX + mgW + mgGap, mgX + 2 * (mgW + mgGap) };
        boolean[] hovArr = { macrosHovered, profilesHovered, configsHovered };
        String[] labArr  = { "Macros", "Profiles", "Configs" };

        for (int i = 0; i < 3; i++) {
            int bx  = bxArr[i];
            boolean h = hovArr[i];
            int bg  = h ? activeColor : 0xFF1C1C2C;

            graphics.fillGradient(bx, mgY, bx + mgW, mgY + mgH, h ? activeColor : 0xFF202038, h ? activeColor : 0xFF161624);

            int textW = FontRenderUtility.getStringWidth(labArr[i]);
            int textY = mgY + (mgH - FontRenderUtility.getFontHeight()) / 2;
            FontRenderUtility.drawString(graphics, labArr[i], bx + (mgW - textW) / 2, textY,
                    h ? 0xFFFFFFFF : 0xFFB0B0CC, false);
        }

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
            tooltipAlpha = Math.min(1.0f, tooltipAlpha + 0.10f);
        } else {
            tooltipAlpha = Math.max(0.0f, tooltipAlpha - 0.15f);
        }

        if (tooltipAlpha > 0.02f && !activeTooltipText.isEmpty()) {
            int tx = mouseX + 8;
            int ty = mouseY + 8;
            int tw = FontRenderUtility.getStringWidth(activeTooltipText) + 6;
            int th = 14;

            if (tx + tw > this.width) tx = mouseX - tw - 4;
            if (ty + th > this.height) ty = mouseY - th - 4;

            int ba = (int)(tooltipAlpha * 238);
            int ta = (int)(tooltipAlpha * 255);
            int bga = Math.min(255, (int)(tooltipAlpha * 100));
            graphics.fill(tx, ty, tx + tw, ty + th, (ba << 24) | 0x141414);
            Render2DEngine.drawBorder(graphics, tx, ty, tw, th, 1, (ta << 24) | 0x2C2C2C);
            FontRenderUtility.drawString(graphics, activeTooltipText, tx + 3, ty + 3, (ta << 24) | 0xE0E0E0, true);
        }

        if (activeColorPalette != null) {
            activeColorPalette.render(graphics, mouseX, mouseY, this.width, this.height);
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    private void renderSearchBar(GuiGraphics graphics, int mouseX, int mouseY) {
        int barW = Math.min(200, this.width - 60);
        int barX = (this.width - barW) / 2;
        int barY = 14;
        int barH = 20;

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
        int expandedW = barW + (int)(40 * eased);
        int actualX = barX - (expandedW - barW) / 2;

        graphics.fillGradient(actualX, barY, actualX + expandedW, barY + barH, 0xF0121228, 0xF00E0E1A);
        Render2DEngine.drawBorder(graphics, actualX, barY, expandedW, barH, 1, ColorUtility.withAlpha(glowColor, 60));
        graphics.fill(actualX, barY + barH - 1, actualX + expandedW, barY + barH, glowColor);

        int iconSize = 14;
        Identifier searchTex = getSearchTexture();
        if (searchTex != null) {
            int iconX = actualX + 8;
            int iconY = barY + (barH - iconSize) / 2;
            graphics.blit(searchTex, iconX, iconY, iconX + iconSize, iconY + iconSize, 0.0f, 1.0f, 0.0f, 1.0f);
        }

        int textOffset = 8 + iconSize + 4;
        String searchText = searchQuery;
        int textY = barY + (barH - FontRenderUtility.getFontHeight()) / 2 + 1;
        if (searchText.isEmpty() && !searchFocused) {
            FontRenderUtility.drawString(graphics, "Search...", actualX + textOffset, textY, 0xFF505068, true);
        } else {
            FontRenderUtility.drawString(graphics, searchText, actualX + textOffset, textY, 0xFFC0C0D0, true);
            if (searchFocused) {
                int textW = FontRenderUtility.getStringWidth(searchText);
                float cursorBlink = (float)Math.sin(searchCursorCounter * 0.1f);
                int cursorAlpha = (int)(150 + 105 * cursorBlink * cursorBlink * cursorBlink);
                graphics.fill(actualX + textOffset + textW, textY - 1, actualX + textOffset + 2 + textW, textY + FontRenderUtility.getFontHeight() + 1, (cursorAlpha << 24) | (glowColor & 0xFFFFFF));
            }
        }

        int resultCount = 0;
        if (!searchQuery.isEmpty()) {
            for (var panel : panels) {
                resultCount += panel.getMatchCount(searchQuery);
            }
            if (resultCount > 0) {
                String countText = String.valueOf(resultCount);
                int cw = FontRenderUtility.getStringWidth(countText);
                FontRenderUtility.drawString(graphics, countText, actualX + expandedW - cw - 8, textY, 0xFF606080, true);
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

        int barW = Math.min(200, this.width - 60);
        int barX = (this.width - barW) / 2;
        int barY = 14;
        int barH = 20;

        if (event.x() >= barX && event.x() <= barX + barW && event.y() >= barY && event.y() <= barY + barH) {
            searchFocused = true;
            return true;
        }

        if (bindingModuleButton != null) {
            return super.mouseClicked(event, handled);
        }

        // Match layout computed in render()
        float btnScale = Math.max(0.65f, getResponsiveScale());
        int mgW   = (int)(60 * btnScale);
        int mgH   = (int)(20 * btnScale);
        int mgGap = (int)(6 * btnScale);
        int mgX   = (this.width - (3 * mgW + 2 * mgGap)) / 2;
        int mgY   = this.height - (int)(38 * btnScale);

        if (event.x() >= mgX && event.x() <= mgX + mgW && event.y() >= mgY && event.y() <= mgY + mgH) {
            this.minecraft.setScreen(new MacroScreen(this));
            return true;
        }
        if (event.x() >= mgX + mgW + mgGap && event.x() <= mgX + 2 * mgW + mgGap && event.y() >= mgY && event.y() <= mgY + mgH) {
            this.minecraft.setScreen(new ProfilesScreen(this));
            return true;
        }
        if (event.x() >= mgX + 2 * (mgW + mgGap) && event.x() <= mgX + 3 * mgW + 2 * mgGap && event.y() >= mgY && event.y() <= mgY + mgH) {
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

        boolean ctrlPressed = (GLFW.glfwGetKey(this.minecraft.getWindow().handle(), GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS) ||
                             (GLFW.glfwGetKey(this.minecraft.getWindow().handle(), GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS);

        if (ctrlPressed && key == GLFW.GLFW_KEY_F) {
            searchFocused = true;
            searchQuery = "";
            return true;
        }

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

        if (activeKeybindElement != null) {
            ravex.parameter.KeybindParameter kp = (ravex.parameter.KeybindParameter) activeKeybindElement.getParameter();
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                kp.setValue(org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN);
                activeKeybindElement = null;
            } else {
                kp.setValue(key);
                activeKeybindElement = null;
            }
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
        if (activeKeybindElement != null) {
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
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (activeColorPalette != null) {
            return false;
        }

        float finalScale = getAdaptiveScale();
        float cx = this.width / 2.0f;
        float cy = this.height / 2.0f;

        double mx = (mouseX - cx) / finalScale + cx;
        double my = (mouseY - cy) / finalScale + cy;

        for (CategoryPanel panel : panels) {
            if (panel.mouseScrolled(mx, my, horizontalAmount, verticalAmount, searchQuery)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void onClose() {
        activeStringParameterElement = null;
        activeKeybindElement = null;
        if (!closing) {
            closing = true;
            closingStartTime = System.currentTimeMillis();
            ravex.manager.LayoutManager.INSTANCE.save(panelMap);
            ravex.utility.sound.SoundUtility.playGuiClose();
            ravex.manager.ConfigManager.INSTANCE.save("default");
            if (ravex.modules.render.ClickGui.INSTANCE.blur.getValue()) {
                BlurUtility.disable();
            }
        }
    }

    // ─── Star particle system ────────────────────────────────────────────────────

    private static final float STAR_PULSE = 0.0008f;
    private static final float STAR_ALPHA_MIN = 0.15f;
    private static final float STAR_ALPHA_RANGE = 0.30f;

    private void renderStars(GuiGraphics graphics) {
        if (this.width <= 0 || this.height <= 0) return;

        if (!starsInit) {
            java.util.Random rng = new java.util.Random(0xDEADBEEFL);
            for (int i = 0; i < STAR_COUNT; i++) {
                starX[i]     = rng.nextFloat() * this.width;
                starY[i]     = rng.nextFloat() * this.height;
                float speedMultiplier = 0.5f + rng.nextFloat() * 1.5f;
                starVx[i]    = (rng.nextFloat() - 0.5f) * 0.15f * speedMultiplier;
                starVy[i]    = (rng.nextFloat() - 0.5f) * 0.15f * speedMultiplier;
                starAlpha[i] = STAR_ALPHA_MIN + rng.nextFloat() * STAR_ALPHA_RANGE;
                starSize[i]  = 1f + rng.nextFloat() * 4f;
            }
            starsInit = true;
        }

        long now   = System.currentTimeMillis();
        float dt   = Math.min(32f, now - lastStarTick);
        if (lastStarTick == 0) dt = 16f;
        lastStarTick = now;

        int accentColor = ColorUtility.getActiveColor();

        for (int i = 0; i < STAR_COUNT; i++) {
            starX[i] += starVx[i] * dt;
            starY[i] += starVy[i] * dt;

            if (starX[i] < 0)            starX[i] += this.width;
            if (starX[i] > this.width)   starX[i] -= this.width;
            if (starY[i] < 0)            starY[i] += this.height;
            if (starY[i] > this.height)  starY[i] -= this.height;

            float pulse = (float)(Math.sin(now * 0.0015 + i * 1.7) * 0.5 + 0.5);
            float currentAlpha = STAR_ALPHA_MIN + pulse * STAR_ALPHA_RANGE;

            int alpha = (int)(currentAlpha * 255);
            int col   = ColorUtility.withAlpha(accentColor, alpha);

            int sx = (int) starX[i];
            int sy = (int) starY[i];
            int sz = Math.max(1, (int) starSize[i]);

            if (sz <= 2) {
                graphics.fill(sx, sy, sx + sz, sy + sz, col);
            } else {
                graphics.fill(sx, sy, sx + sz, sy + sz, col);
                int glowAlpha1 = Math.max(0, alpha / 3);
                int glowCol1 = ColorUtility.withAlpha(accentColor, glowAlpha1);
                graphics.fill(sx - 1, sy - 1, sx + sz + 1, sy + sz + 1, glowCol1);

                int glowAlpha2 = Math.max(0, alpha / 7);
                int glowCol2 = ColorUtility.withAlpha(accentColor, glowAlpha2);
                graphics.fill(sx - 2, sy - 2, sx + sz + 2, sy + sz + 2, glowCol2);
            }
        }
    }
}
