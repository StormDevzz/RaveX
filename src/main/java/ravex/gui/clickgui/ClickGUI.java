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
import ravex.manager.ModuleManager;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DEngine;
import ravex.utility.render.animate.AnimationUtility;

import java.util.ArrayList;
import java.util.List;

import java.util.stream.Collectors;

public class ClickGUI extends Screen {
    public static ModuleButton bindingModuleButton = null;
    public static String hoveredDescription = null;
    public static String searchQuery = "";
    public static ravex.parameter.ColorParameter activeColorParameter = null;
    public static ColorPaletteModal activeColorPalette = null;
    public static ParameterElement activeStringParameterElement = null;
    public static ParameterElement activeNumberParameterElement = null;
    public static ParameterElement activeKeybindElement = null;
    public static boolean isDraggingSlider = false;

    public static Identifier getCategoryTexture(Category cat) {
        return ravex.utility.render.TextureLoader.getCategoryTexture(cat);
    }

    public static Identifier getSearchTexture() {
        return ravex.utility.render.TextureLoader.getSearchTexture();
    }


    private final List<CategoryPanel> panels = new ArrayList<>();
    private final long initTime;
    private double smoothedMaxH = 200.0;
    private int panelStartY = 65;
    private float currentScale = -1;

    private boolean closing = false;
    private long closingStartTime = 0;
    private float tooltipAlpha = 0.0f;
    private String activeTooltipText = "";

    private boolean searchFocused = false;
    private float searchAnimProgress = 0f;
    private float searchResultAnim = 0f;
    private long searchLastUpdate = System.currentTimeMillis();
    private String searchBeforeEdit = "";
    private int searchCursorCounter = 0;
    private float searchBarOpenAnim = 0f;
<<<<<<< HEAD
    private float descPanelAnim = 0f;
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3

    private boolean macrosHovered;
    private boolean profilesHovered;
    private boolean configsHovered;
    private boolean resetLayoutHovered;
<<<<<<< HEAD
    private boolean hudEditorHovered;
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3

    private final float[] starX   = new float[120];
    private final float[] starY   = new float[120];
    private final float[] starVx  = new float[120];
    private final float[] starVy  = new float[120];
    private final float[] starAlpha = new float[120];
    private final float[] starSize  = new float[120];
    private boolean starsInit = false;
    private String lastParticleType = "";
    private float lastParticleSpeed = 0;
    private float lastParticleSize = 0;

    private long lastStarTick = 0;
    private int cachedActiveColor = 0xFF40A9F8;

    public ClickGUI() {
        super(Component.literal("RaveX ClickGUI"));
        this.initTime = System.currentTimeMillis();

        ravex.utility.misc.GuiOptimizer.optimize();

<<<<<<< HEAD
        int panelW = ModuleManager.get(ravex.modules.client.ClickGui.class).panelWidth.getValue().intValue();
=======
        
        ravex.utility.render.TextureLoader.preloadAll();

        int panelW = ravex.modules.client.ClickGui.INSTANCE.panelWidth.getValue().intValue();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        int spacing = 10;
        int panelIndex = 0;
        for (Category cat : Category.values()) {
            boolean hasModules = ModuleManager.INSTANCE.getByCategory(cat).stream().anyMatch(m -> !m.isHud());
            if (!hasModules) continue;
            int px = 20 + panelIndex * (panelW + spacing);
            int py = 65;
            CategoryPanel p = new CategoryPanel(cat, px, py);
            panels.add(p);
            panelIndex++;
        }
    }

    @Override
    protected void init() {
<<<<<<< HEAD
        int panelW = ModuleManager.get(ravex.modules.client.ClickGui.class).panelWidth.getValue().intValue();
=======
        int panelW = ravex.modules.client.ClickGui.INSTANCE.panelWidth.getValue().intValue();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        int spacing = 10;
        int num = panels.size();

        float totalW = num * panelW + (num - 1) * spacing;
        float startX = Math.max(10, (this.width - totalW) / 2f);
        float startY = Math.max(65, (this.height - Math.min(this.height * 0.55f, getMaxPanelHeight())) / 2f);
        this.panelStartY = (int) startY;

        for (int i = 0; i < num; i++) {
            int px = (int) (startX + i * (panelW + spacing));
            int py = (int) startY;
            panels.get(i).setX(px);
            panels.get(i).setY(py);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int getMaxPanelHeight() {
        int maxH = 150;
        for (CategoryPanel panel : panels) {
            int h = panel.getBaseHeight(searchQuery);
            if (h > maxH) maxH = h;
        }
        return maxH;
    }

    private float getResponsiveScale() {
<<<<<<< HEAD
        float target = ModuleManager.get(ravex.modules.client.ClickGui.class).guiScale.getValue().floatValue();
=======
        float target = ravex.modules.client.ClickGui.INSTANCE.guiScale.getValue().floatValue();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (currentScale < 0) {
            currentScale = target;
        }
        if (isDraggingSlider) {
            
        } else {
            
            currentScale += (target - currentScale) * 0.15f;
            if (Math.abs(currentScale - target) < 0.001f) {
                currentScale = target;
            }
        }
        return currentScale;
    }

    private float getAdaptiveScale() {
        long elapsed = System.currentTimeMillis() - initTime;
        float animProgress = Math.min(1.0f, elapsed / 120.0f);
        float animScale;
        if (animProgress < 0.5f) {
            animScale = 2 * animProgress * animProgress;
        } else {
            animScale = 1 - (float)Math.pow(-2 * animProgress + 2, 2) / 2;
        }

        if (closing) {
            long closingElapsed = System.currentTimeMillis() - closingStartTime;
            if (closingElapsed < 150) {
                float closingProgress = closingElapsed / 150.0f;
                float easeIn = closingProgress * closingProgress;
                animScale = 1.0f + easeIn * 1.5f;
            }
        }

        float responsiveScale = getResponsiveScale();
        return animScale * responsiveScale;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int originalMouseX = mouseX;
        int originalMouseY = mouseY;
        if (activeColorPalette != null) {
            mouseX = -9999;
            mouseY = -9999;
        }
        ModuleButton.tickAllGears();
        int targetMaxH = getMaxPanelHeight();
        smoothedMaxH += (targetMaxH - smoothedMaxH) * 0.35;

        cachedActiveColor = ColorUtility.getActiveColor();

<<<<<<< HEAD
        boolean drawBg = ModuleManager.get(ravex.modules.client.ClickGui.class).drawBackground.getValue();
        if (drawBg) {
            int bgOpacity = ModuleManager.get(ravex.modules.client.ClickGui.class).backgroundOpacity.getValue().intValue();
            graphics.fillGradient(0, 0, this.width, this.height, ColorUtility.withAlpha(ColorUtility.BACKGROUND_START, bgOpacity), ColorUtility.withAlpha(ColorUtility.BACKGROUND_END, bgOpacity));
        }

        if (ModuleManager.get(ravex.modules.client.GuiParticles.class).getEnabled()) {
            renderStars(graphics);
        }

        if (ModuleManager.get(ravex.modules.client.ClickGui.class).companionImage.getValue()) {
            String type = ModuleManager.get(ravex.modules.client.ClickGui.class).companionType.getValue();
=======
        boolean drawBg = ravex.modules.client.ClickGui.INSTANCE.drawBackground.getValue();
        if (drawBg) {
            int bgOpacity = ravex.modules.client.ClickGui.INSTANCE.backgroundOpacity.getValue().intValue();
            graphics.fillGradient(0, 0, this.width, this.height, ColorUtility.withAlpha(ColorUtility.BACKGROUND_START, bgOpacity), ColorUtility.withAlpha(ColorUtility.BACKGROUND_END, bgOpacity));
        }

        if (ravex.modules.client.GuiParticles.INSTANCE.getEnabled()) {
            renderStars(graphics);
        }

        if (ravex.modules.client.ClickGui.INSTANCE.companionImage.getValue()) {
            String type = ravex.modules.client.ClickGui.INSTANCE.companionType.getValue();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            Identifier imgId = null;
            if ("Femboy".equals(type)) {
                imgId = ravex.utility.render.TextureLoader.FEMBOY;
            } else if ("Wypher1".equals(type)) {
                imgId = ravex.utility.render.TextureLoader.WYPHER1;
            } else if ("Boykgun".equals(type)) {
                imgId = ravex.utility.render.TextureLoader.BOYKGUN;
            } else if ("Cutie".equals(type)) {
                imgId = ravex.utility.render.TextureLoader.CUTIE;
            } else if ("Kiss".equals(type)) {
                imgId = ravex.utility.render.TextureLoader.KISS;
            } else if ("Laying".equals(type)) {
                imgId = ravex.utility.render.TextureLoader.LAYING;
            } else if ("Licking".equals(type)) {
                imgId = ravex.utility.render.TextureLoader.LICKING;
            } else if ("Pillow".equals(type)) {
                imgId = ravex.utility.render.TextureLoader.PILLOW;
            } else if ("Cutieeee".equals(type)) {
                imgId = ravex.utility.render.TextureLoader.CUTIEEEE;
            } else if ("Cutiemonster".equals(type)) {
                imgId = ravex.utility.render.TextureLoader.CUTIEMONSTER;
            } else if ("Furik".equals(type)) {
                imgId = ravex.utility.render.TextureLoader.FURIK;
            } else if ("Godofcoding".equals(type)) {
                imgId = ravex.utility.render.TextureLoader.GODOFCODING;
            } else if ("Terrydavis".equals(type)) {
                imgId = ravex.utility.render.TextureLoader.TERRYDAVIS;
            }

            if (imgId != null) {
                int imgW = 120;
                int imgH = 120;
                int imgX = this.width - imgW - 10;
                int imgY = this.height - imgH - 45;

                graphics.blit(imgId, imgX, imgY, imgX + imgW, imgY + imgH, 0.0f, 1.0f, 0.0f, 1.0f);
            }
        }

<<<<<<< HEAD
        if (ModuleManager.get(ravex.modules.client.ClickGui.class).showToolbar.getValue()) {
=======
        if (ravex.modules.client.ClickGui.INSTANCE.showToolbar.getValue()) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            float btnScale = Math.max(0.65f, getResponsiveScale());
            int mgW = (int)(44 * btnScale);
            int mgH = (int)(20 * btnScale);
            int mgGap = (int)(40 * btnScale);
<<<<<<< HEAD
            int totalBtnW = 5 * mgW + 4 * mgGap;
=======
            int totalBtnW = 4 * mgW + 3 * mgGap;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            int mgX = (this.width - totalBtnW) / 2;
            int mgY = Math.max(4, panelStartY - mgH - 6);

            macrosHovered      = mouseX >= mgX && mouseX <= mgX + mgW && mouseY >= mgY && mouseY <= mgY + mgH;
            profilesHovered    = mouseX >= mgX + mgW + mgGap && mouseX <= mgX + 2 * mgW + mgGap && mouseY >= mgY && mouseY <= mgY + mgH;
            configsHovered     = mouseX >= mgX + 2 * (mgW + mgGap) && mouseX <= mgX + 3 * mgW + 2 * mgGap && mouseY >= mgY && mouseY <= mgY + mgH;
            resetLayoutHovered = mouseX >= mgX + 3 * (mgW + mgGap) && mouseX <= mgX + 4 * mgW + 3 * mgGap && mouseY >= mgY && mouseY <= mgY + mgH;
<<<<<<< HEAD
            hudEditorHovered   = mouseX >= mgX + 4 * (mgW + mgGap) && mouseX <= mgX + 5 * mgW + 4 * mgGap && mouseY >= mgY && mouseY <= mgY + mgH;

            int[] bxArr   = { mgX, mgX + mgW + mgGap, mgX + 2 * (mgW + mgGap), mgX + 3 * (mgW + mgGap), mgX + 4 * (mgW + mgGap) };
            boolean[] hovArr = { macrosHovered, profilesHovered, configsHovered, resetLayoutHovered, hudEditorHovered };
            String[] labArr  = { "Macros", "Profiles", "Configs", "Reset", "HUD" };

            int btnR = Math.min(8, mgH / 2);
            for (int i = 0; i < 5; i++) {
=======

            int[] bxArr   = { mgX, mgX + mgW + mgGap, mgX + 2 * (mgW + mgGap), mgX + 3 * (mgW + mgGap) };
            boolean[] hovArr = { macrosHovered, profilesHovered, configsHovered, resetLayoutHovered };
            String[] labArr  = { "Macros", "Profiles", "Configs", "Reset" };

            int btnR = Math.min(8, mgH / 2);
            for (int i = 0; i < 4; i++) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                int bx  = bxArr[i];
                boolean h = hovArr[i];

                Render2DEngine.drawRound(graphics, bx, mgY, mgW, mgH, btnR, h ? ColorUtility.withAlpha(cachedActiveColor, 40) : 0x18000000);
                if (h) {
                    Render2DEngine.drawRound(graphics, bx, mgY, mgW, mgH, btnR, ColorUtility.withAlpha(cachedActiveColor, 20));
                }

                int textW = FontRenderUtility.getStringWidth(labArr[i]);
                int textY = mgY + (mgH - FontRenderUtility.getFontHeight()) / 2;
                FontRenderUtility.drawString(graphics, labArr[i], bx + (mgW - textW) / 2, textY,
                        h ? 0xFFFFFFFF : 0xFF808090, false);
            }
        }

        hoveredDescription = null;

        float finalScale = getAdaptiveScale();
        if (closing && (System.currentTimeMillis() - closingStartTime >= 150)) {
            this.minecraft.setScreen(null);
            return;
        }

        float cx = this.width / 2.0f;
        float cy = this.height / 2.0f;

        int mx = (int) ((mouseX - cx) / finalScale + cx);
        int my = (int) ((mouseY - cy) / finalScale + cy);

        if (closing) {
            float elapsed = System.currentTimeMillis() - closingStartTime;
            float progress = Math.min(1f, elapsed / 120f);
            searchBarOpenAnim = 1f - AnimationUtility.Easing.QUINT_IN.apply(progress);
<<<<<<< HEAD
            descPanelAnim = 1f - AnimationUtility.Easing.QUINT_IN.apply(progress);
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        } else {
            float elapsed = System.currentTimeMillis() - initTime;
            float progress = Math.min(1f, elapsed / 400f);
            searchBarOpenAnim = AnimationUtility.Easing.ELASTIC_OUT.apply(progress);
<<<<<<< HEAD
            float dpProgress = Math.min(1f, elapsed / 250f);
            descPanelAnim = dpProgress;
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        }

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

<<<<<<< HEAD
        if (ModuleManager.get(ravex.modules.client.ClickGui.class).descriptionPanel.getValue()) {
            activeTooltipText = hoveredDescription != null ? hoveredDescription : "";
            if (!activeTooltipText.isEmpty()) {
                int maxW = 200;
                List<String> lines = wrapText(activeTooltipText, maxW);
                int lineH = FontRenderUtility.getFontHeight() + 2;
                int padX = 8;
                int padY = 6;
                int tw = 0;
                for (String line : lines) {
                    int lw = FontRenderUtility.getStringWidth(line);
                    if (lw > tw) tw = lw;
                }
                tw = Math.min(tw + padX * 2, maxW + padX * 2);
                int th = lines.size() * lineH;

                int barH = 20;
                int barY = Math.max(4, panelStartY - (int)(20 * Math.max(0.65f, getResponsiveScale())) - 2 - barH);
                barY += (int)((1f - searchBarOpenAnim) * -18f);

                int descY = barY + barH + 4;
                int descX = (this.width - tw) / 2;

                int da = (int)(descPanelAnim * 255);

                int ly = descY + padY;
                for (String line : lines) {
                    FontRenderUtility.drawString(graphics, line, descX + padX, ly, (da << 24) | 0xE0E0E0, true);
                    ly += lineH;
                }
            }
        } else {
            if (hoveredDescription != null) {
                activeTooltipText = hoveredDescription;
                float speed = ModuleManager.get(ravex.modules.client.ClickGui.class).tooltipSpeed.getValue().floatValue() / 10f;
                tooltipAlpha = Math.min(1.0f, tooltipAlpha + 0.10f * speed);
            } else {
                float speed = ModuleManager.get(ravex.modules.client.ClickGui.class).tooltipSpeed.getValue().floatValue() / 10f;
                tooltipAlpha = Math.max(0.0f, tooltipAlpha - 0.15f * speed);
            }

            if (tooltipAlpha > 0.02f && !activeTooltipText.isEmpty()) {
                int ox = ModuleManager.get(ravex.modules.client.ClickGui.class).tooltipOffsetX.getValue().intValue();
                int oy = ModuleManager.get(ravex.modules.client.ClickGui.class).tooltipOffsetY.getValue().intValue();
                int maxW = 200;
                List<String> lines = wrapText(activeTooltipText, maxW);
                int lineH = FontRenderUtility.getFontHeight() + 2;
                int padX = 8;
                int padY = 6;
                int tw = 0;
                for (String line : lines) {
                    int lw = FontRenderUtility.getStringWidth(line);
                    if (lw > tw) tw = lw;
                }
                tw = Math.min(tw + padX * 2, maxW + padX * 2);
                int th = lines.size() * lineH + padY * 2;

                int tx = mouseX + ox;
                int ty = mouseY + oy;
                if (tx + tw > this.width) tx = mouseX - tw - 4;
                if (ty + th > this.height) ty = mouseY - th - 4;
                if (tx < 0) tx = 2;
                if (ty < 0) ty = 2;

                int to = ModuleManager.get(ravex.modules.client.ClickGui.class).descriptionOpacity.getValue().intValue();
                int ba = (int)(tooltipAlpha * to);
                int ta = (int)(tooltipAlpha * 255);

                //Render2DEngine.drawRound(graphics, tx, ty, tw, th, 6, (ba << 24) | 0x0A0A10);
                //Render2DEngine.drawRound(graphics, tx + 1, ty + 1, tw - 2, th - 2, 5, (ba << 24) | 0x151520);

                int ly = ty + padY;
                for (String line : lines) {
                    FontRenderUtility.drawString(graphics, line, tx + padX, ly, (ta << 24) | 0xE0E0E0, true);
                    ly += lineH;
                }
=======
        if (hoveredDescription != null) {
            activeTooltipText = hoveredDescription;
            float speed = ravex.modules.client.ClickGui.INSTANCE.tooltipSpeed.getValue().floatValue() / 10f;
            tooltipAlpha = Math.min(1.0f, tooltipAlpha + 0.10f * speed);
        } else {
            float speed = ravex.modules.client.ClickGui.INSTANCE.tooltipSpeed.getValue().floatValue() / 10f;
            tooltipAlpha = Math.max(0.0f, tooltipAlpha - 0.15f * speed);
        }

        if (tooltipAlpha > 0.02f && !activeTooltipText.isEmpty()) {
            int ox = ravex.modules.client.ClickGui.INSTANCE.tooltipOffsetX.getValue().intValue();
            int oy = ravex.modules.client.ClickGui.INSTANCE.tooltipOffsetY.getValue().intValue();
            int maxW = 200;
            List<String> lines = wrapText(activeTooltipText, maxW);
            int lineH = FontRenderUtility.getFontHeight() + 2;
            int padX = 8;
            int padY = 6;
            int tw = 0;
            for (String line : lines) {
                int lw = FontRenderUtility.getStringWidth(line);
                if (lw > tw) tw = lw;
            }
            tw = Math.min(tw + padX * 2, maxW + padX * 2);
            int th = lines.size() * lineH + padY * 2;

            int tx = mouseX + ox;
            int ty = mouseY + oy;
            if (tx + tw > this.width) tx = mouseX - tw - 4;
            if (ty + th > this.height) ty = mouseY - th - 4;
            if (tx < 0) tx = 2;
            if (ty < 0) ty = 2;

            int to = ravex.modules.client.ClickGui.INSTANCE.descriptionOpacity.getValue().intValue();
            int ba = (int)(tooltipAlpha * to);
            int ta = (int)(tooltipAlpha * 255);

            Render2DEngine.drawRound(graphics, tx, ty, tw, th, 6, (ba << 24) | 0x0A0A10);
            Render2DEngine.drawRound(graphics, tx + 1, ty + 1, tw - 2, th - 2, 5, (ba << 24) | 0x151520);

            int ly = ty + padY;
            for (String line : lines) {
                FontRenderUtility.drawString(graphics, line, tx + padX, ly, (ta << 24) | 0xE0E0E0, true);
                ly += lineH;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            }
        }

        if (activeColorPalette != null) {
            activeColorPalette.render(graphics, originalMouseX, originalMouseY, this.width, this.height);
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    private void renderSearchBar(GuiGraphics graphics, int mouseX, int mouseY) {
        float openAnim = searchBarOpenAnim;
        if (openAnim <= 0.01f) return;

        int barH = 20;
        int barY = Math.max(4, panelStartY - (int)(20 * Math.max(0.65f, getResponsiveScale())) - 2 - barH);
        barY += (int)((1f - openAnim) * -18f);
        int barW = Math.min(200, this.width - 60);
        int barX = (this.width - barW) / 2;

        float target = searchFocused ? 1.0f : 0.0f;
        searchAnimProgress += (target - searchAnimProgress) * 0.12f;
        searchCursorCounter++;

<<<<<<< HEAD
        int pAlpha = ModuleManager.get(ravex.modules.client.ClickGui.class).panelOpacity.getValue().intValue();
=======
        int pAlpha = ravex.modules.client.ClickGui.INSTANCE.panelOpacity.getValue().intValue();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        int barBg = ColorUtility.withAlpha(ColorUtility.PANEL_BODY_END, (int)((searchFocused ? Math.min(pAlpha + 15, 255) : pAlpha) * openAnim));
        Render2DEngine.drawRound(graphics, barX, barY, barW, barH, 10, barBg);

        int iconSize = 14;
        Identifier searchTex = ravex.utility.render.TextureLoader.getSearchWhiteTexture();
        if (searchTex != null) {
            int iconX = barX + 8;
            int iconY = barY + (barH - iconSize) / 2;
            graphics.blit(searchTex, iconX, iconY, iconX + iconSize, iconY + iconSize, 0.0f, 1.0f, 0.0f, 1.0f);
        }

        int textOffset = 8 + iconSize + 4;
        String searchText = searchQuery;
        int textY = barY + (barH - FontRenderUtility.getFontHeight()) / 2 + 1;
        int textColor = lerpColor(0xFF606080, 0xFFD0D0E0, searchAnimProgress);
        if (searchText.isEmpty() && !searchFocused) {
            FontRenderUtility.drawString(graphics, "Search...", barX + textOffset, textY, textColor, true);
        } else {
            FontRenderUtility.drawString(graphics, searchText, barX + textOffset, textY, textColor, true);
            if (searchFocused) {
                int textW = FontRenderUtility.getStringWidth(searchText);
                boolean cursorOn = (searchCursorCounter / 30) % 2 == 0;
                if (cursorOn) {
                    int cursorAlpha = (int)(0xC8 * openAnim);
                    graphics.fill(barX + textOffset + textW, textY - 1, barX + textOffset + 2 + textW, textY + FontRenderUtility.getFontHeight() + 1,
                        (cursorAlpha << 24) | 0xFFFFFF);
                }
            }
        }

        int resultCount = 0;
        if (!searchQuery.isEmpty()) {
            for (var panel : panels) {
                resultCount += panel.getMatchCount(searchQuery);
            }
        }
        float resultTarget = (!searchQuery.isEmpty() && resultCount > 0) ? 1.0f : 0.0f;
        searchResultAnim += (resultTarget - searchResultAnim) * 0.10f;
        if (searchResultAnim > 0.01f) {
            String countText = String.valueOf(resultCount);
            int cw = FontRenderUtility.getStringWidth(countText);
            int ra = (int)(searchResultAnim * 180);
            FontRenderUtility.drawString(graphics, countText, barX + barW - cw - 8, textY, (ra << 24) | 0xA0A0C0, true);
            if (searchResultAnim > 0.95f) {
                graphics.fill(barX + barW - cw - 10, textY - 1, barX + barW - cw - 8, textY + FontRenderUtility.getFontHeight() + 1,
                    ColorUtility.withAlpha(ColorUtility.PANEL_BODY_END, (int)(Math.min(pAlpha + 30, 200) * openAnim)));
            }
        }
    }

    private int lerpColor(int bg, int fg, float alpha) {
        int aBg = (bg >> 24) & 0xFF, rBg = (bg >> 16) & 0xFF, gBg = (bg >> 8) & 0xFF, bBg = bg & 0xFF;
        int aFg = (fg >> 24) & 0xFF, rFg = (fg >> 16) & 0xFF, gFg = (fg >> 8) & 0xFF, bFg = fg & 0xFF;
        int r = (int)(rBg + (rFg - rBg) * alpha);
        int g = (int)(gBg + (gFg - gBg) * alpha);
        int b = (int)(bBg + (bFg - bBg) * alpha);
        int a = (int)(aBg + (aFg - aBg) * alpha);
        return (a << 24) | (r << 16) | (g << 8) | b;
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
        float btnScaleM = Math.max(0.65f, getResponsiveScale());
        int mgHForCalc = (int)(20 * btnScaleM);
        int barH = 20;
        int barY = Math.max(4, panelStartY - mgHForCalc - 6 - 4 - barH);

        if (event.x() >= barX && event.x() <= barX + barW && event.y() >= barY && event.y() <= barY + barH) {
            searchFocused = true;
            return true;
        }

        if (bindingModuleButton != null) {
            return super.mouseClicked(event, handled);
        }

<<<<<<< HEAD
        if (ModuleManager.get(ravex.modules.client.ClickGui.class).showToolbar.getValue()) {
=======
        if (ravex.modules.client.ClickGui.INSTANCE.showToolbar.getValue()) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            float btnScale = Math.max(0.65f, getResponsiveScale());
            int mgW   = (int)(44 * btnScale);
            int mgH   = (int)(20 * btnScale);
            int mgGap = (int)(40 * btnScale);
<<<<<<< HEAD
            int mgX   = (this.width - (5 * mgW + 4 * mgGap)) / 2;
=======
            int mgX   = (this.width - (4 * mgW + 3 * mgGap)) / 2;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            int mgY   = Math.max(4, panelStartY - mgH - 6);

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
            if (event.x() >= mgX + 3 * (mgW + mgGap) && event.x() <= mgX + 4 * mgW + 3 * mgGap && event.y() >= mgY && event.y() <= mgY + mgH) {
                ravex.manager.LayoutManager.INSTANCE.reset();
                init();
                return true;
            }
<<<<<<< HEAD
            if (event.x() >= mgX + 4 * (mgW + mgGap) && event.x() <= mgX + 5 * mgW + 4 * mgGap && event.y() >= mgY && event.y() <= mgY + mgH) {
                this.minecraft.setScreen(new ravex.gui.hudeditor.HudEditorScreen(this));
                return true;
            }
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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
            return true;
        }

        if (activeNumberParameterElement != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER) {
                activeNumberParameterElement.applyInput();
                activeNumberParameterElement = null;
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                activeNumberParameterElement.removeLastChar();
                return true;
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
            String text = event.codepointAsString();
            if (!text.isEmpty()) {
                return activeColorPalette.charTyped(text.charAt(0));
            }
            return true;
        }
        if (activeKeybindElement != null) {
            return true;
        }
        if (activeNumberParameterElement != null) {
            String text = event.codepointAsString();
            if (!text.isEmpty()) {
                char ch = text.charAt(0);
                if ((ch >= '0' && ch <= '9') || ch == '.' || ch == '-') {
                    activeNumberParameterElement.appendChar(ch);
                }
            }
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
        isDraggingSlider = false;
        ModuleButton.expandedModules.clear();
        for (ravex.modules.Module m : ravex.manager.ModuleManager.INSTANCE.getModules()) {
            m.setGearAngle(0f, System.currentTimeMillis());
        }
        activeStringParameterElement = null;
        if (activeNumberParameterElement != null) {
            activeNumberParameterElement.applyInput();
            activeNumberParameterElement = null;
        }
        activeKeybindElement = null;
        if (!closing) {
            closing = true;
            closingStartTime = System.currentTimeMillis();
            ravex.manager.ConfigManager.INSTANCE.save("default");
        }
    }

    @Override
    public void removed() {
        isDraggingSlider = false;
        if (activeNumberParameterElement != null) {
            activeNumberParameterElement.applyInput();
            activeNumberParameterElement = null;
        }
        for (ravex.modules.Module m : ravex.manager.ModuleManager.INSTANCE.getModules()) {
            m.setGearAngle(0f, System.currentTimeMillis());
        }
        super.removed();
    }

    private static List<String> wrapText(String text, int maxWidthPx) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }
        String[] raw = text.split("\n");
        for (String segment : raw) {
            if (FontRenderUtility.getStringWidth(segment) <= maxWidthPx) {
                lines.add(segment);
                continue;
            }
            StringBuilder line = new StringBuilder();
            for (String word : segment.split(" ")) {
                String test = line.isEmpty() ? word : line + " " + word;
                if (FontRenderUtility.getStringWidth(test) > maxWidthPx && !line.isEmpty()) {
                    lines.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(test);
                }
            }
            if (!line.isEmpty()) lines.add(line.toString());
        }
        return lines;
    }

    private void renderStars(GuiGraphics graphics) {
        if (this.width <= 0 || this.height <= 0) return;

<<<<<<< HEAD
        var gp = ModuleManager.get(ravex.modules.client.GuiParticles.class);
=======
        var gp = ravex.modules.client.GuiParticles.INSTANCE;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        int count = gp.amount.getValue().intValue();
        count = Math.min(count, 120);
        String pType = gp.type.getValue();
        int pColor = gp.color.getValue();
        float pSize = gp.size.getValue().floatValue();
        float pSpeed = gp.speed.getValue().floatValue();

        boolean reinit = !starsInit
            || !pType.equals(lastParticleType)
            || Math.abs(pSpeed - lastParticleSpeed) > 0.01f
            || Math.abs(pSize - lastParticleSize) > 0.01f;

        if (reinit) {
            starsInit = true;
            lastParticleType = pType;
            lastParticleSpeed = pSpeed;
            lastParticleSize = pSize;
            java.util.Random rng = new java.util.Random(0xDEADBEEFL);
            for (int i = 0; i < 120; i++) {
                starX[i]     = rng.nextFloat() * this.width;
                starY[i]     = rng.nextFloat() * this.height;
                float s = 0.5f + rng.nextFloat() * 1.5f;
                starVx[i]    = (rng.nextFloat() - 0.5f) * 0.15f * s * pSpeed;
                starVy[i]    = (rng.nextFloat() - 0.5f) * 0.15f * s * pSpeed;
                starAlpha[i] = 0.15f + rng.nextFloat() * 0.30f;
                starSize[i]  = Math.max(0.5f, 1f + rng.nextFloat() * 4f + (pSize - 3f));
            }
        }

        long now   = System.currentTimeMillis();
        float dt   = Math.min(32f, now - lastStarTick);
        if (lastStarTick == 0) dt = 16f;
        lastStarTick = now;

        Identifier particleTex = ravex.utility.render.TextureLoader.getParticleTexture(pType, pColor);

        int pR = (pColor >> 16) & 0xFF;
        int pG = (pColor >> 8) & 0xFF;
        int pB = pColor & 0xFF;

        for (int i = 0; i < count; i++) {
            starX[i] += starVx[i] * dt;
            starY[i] += starVy[i] * dt;

            if (starX[i] < 0)            starX[i] += this.width;
            if (starX[i] > this.width)   starX[i] -= this.width;
            if (starY[i] < 0)            starY[i] += this.height;
            if (starY[i] > this.height)  starY[i] -= this.height;

            float pulse = (float)(Math.sin(now * 0.0015 + i * 1.7) * 0.5 + 0.5);
            float currentAlpha = 0.15f + pulse * 0.30f;

            int alpha = (int)(currentAlpha * 255);
            int sx = (int) starX[i];
            int sy = (int) starY[i];
            int sz = Math.max(4, (int)(starSize[i] * 2));

            if (particleTex != null) {
                graphics.blit(particleTex, sx, sy, sx + sz, sy + sz, 0.0f, 1.0f, 0.0f, 1.0f);
            } else {
                int col = (alpha << 24) | (pR << 16) | (pG << 8) | pB;
                graphics.fill(sx, sy, sx + sz, sy + sz, col);
            }
        }
    }
}
