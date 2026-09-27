package ravex.gui.clickgui;
import ravex.utility.misc.ScreenUtility;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import ravex.utility.render.ColorUtility;
import ravex.manager.ModuleManager;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DUtility;
import ravex.utility.render.animate.AnimationUtility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ravex.manager.LayoutManager;

import ravex.modules.Modules;

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
    public static float bindReveal = 0f;

    public static Identifier getCategoryTexture(String cat) {
        return ravex.utility.render.TextureLoaderUtility.getCategoryTexture(cat);
    }

    public static Identifier getSearchTexture() {
        return ravex.utility.render.TextureLoaderUtility.getSearchTexture();
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
    private float toolbarAnim = 0f;
    private float descPanelAnim = 0f;
    private float tipsAnim = 0f;
    private float gearTipAnim = 0f;

    private boolean macrosHovered;
    private boolean profilesHovered;
    private boolean configsHovered;
    private boolean resetLayoutHovered;
    private boolean hudEditorHovered;

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
        super(Component.literal(ravex.utility.misc.LanguageUtility.t("gui_title")));
        this.initTime = System.currentTimeMillis();

        int panelW = (int) ModuleManager.get(ravex.modules.client.ClickGui.class).panelWidth;
        int spacing = 10;
        int panelIndex = 0;
        for (String cat : new String[]{"Combat","Render","Player","Movement","Misc","World","Client","HUD","Custom"}) {
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
        int panelW = (int) ModuleManager.get(ravex.modules.client.ClickGui.class).panelWidth;
        int spacing = 10;
        int num = panels.size();

        float totalW = num * panelW + (num - 1) * spacing;
        float startX = Math.max(10, (this.width - totalW) / 2f);
        float startY = Math.max(65, (this.height - Math.min(this.height * 0.75f, getMaxPanelHeight())) / 2f);
        this.panelStartY = (int) startY;

        Map<String, double[]> layout = LayoutManager.INSTANCE.load();

        for (int i = 0; i < num; i++) {
            CategoryPanel p = panels.get(i);
            if (p.isCustomPosition()) {
                continue;
            }
            if (layout.containsKey(p.getCategory())) {
                double[] pos = layout.get(p.getCategory());
                p.setX((int) Math.round(pos[0]));
                p.setY((int) Math.round(pos[1]));
                p.setCustomPosition(true);
            } else {
                int px = (int) (startX + i * (panelW + spacing));
                int py = (int) startY;
                p.setX(px);
                p.setY(py);
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
            int h = panel.getBaseHeight(searchQuery);
            if (h > maxH) maxH = h;
        }
        return maxH;
    }

    private float getResponsiveScale() {
        float target = (float) ModuleManager.get(ravex.modules.client.ClickGui.class).guiScale;
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

    private int getToolbarHeight() {
        return (int)(20 * Math.max(0.65f, getResponsiveScale()));
    }

    private int getToolbarY() {
        return Math.max(4, panelStartY - getToolbarHeight() - 14);
    }

    private int getSearchBarHeight() {
        return 20;
    }

    private int getSearchBarY() {
        int openOffset = (int)((1f - searchBarOpenAnim) * -18f);
        return Math.max(4, getToolbarY() - getSearchBarHeight() - 8) + openOffset;
    }

    private int[] getToolbarLayout() {
        float btnScale = Math.max(0.65f, getResponsiveScale());
        int mgH = (int)(20 * btnScale);
        int mgGap = (int)(40 * btnScale);
        String[] labs = { ravex.utility.misc.LanguageUtility.t("gui_macros"), ravex.utility.misc.LanguageUtility.t("gui_profiles"), ravex.utility.misc.LanguageUtility.t("gui_configs"), ravex.utility.misc.LanguageUtility.t("gui_reset"), ravex.utility.misc.LanguageUtility.t("gui_hud") };
        int textPad = 12;
        int maxTextW = 0;
        for (String lab : labs) {
            maxTextW = Math.max(maxTextW, FontRenderUtility.getStringWidth(lab));
        }
        int mgW = Math.max((int)(44 * btnScale), maxTextW + textPad * 2);
        int totalBtnW = 5 * mgW + 4 * mgGap;
        int mgX = (this.width - totalBtnW) / 2;
        int mgY = getToolbarY();
        return new int[]{ mgX, mgY, mgW, mgH, mgGap };
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

        boolean drawBg = ModuleManager.get(ravex.modules.client.ClickGui.class).drawBackground;
        if (drawBg) {
            int bgOpacity = (int) ModuleManager.get(ravex.modules.client.ClickGui.class).backgroundOpacity;
            graphics.fillGradient(0, 0, this.width, this.height, ColorUtility.withAlpha(ColorUtility.BACKGROUND_START, bgOpacity), ColorUtility.withAlpha(ColorUtility.BACKGROUND_END, bgOpacity));
        }

        if (ModuleManager.isEnabled(ravex.modules.client.GuiParticles.class)) {
            renderStars(graphics);
        }

        if (ModuleManager.get(ravex.modules.client.ClickGui.class).companionImage) {
            String type = ModuleManager.get(ravex.modules.client.ClickGui.class).companionType;
            Identifier imgId = null;
            if ("Femboy".equals(type)) {
                imgId = ravex.utility.render.TextureLoaderUtility.FEMBOY;
            } else if ("Wypher1".equals(type)) {
                imgId = ravex.utility.render.TextureLoaderUtility.WYPHER1;
            } else if ("Boykgun".equals(type)) {
                imgId = ravex.utility.render.TextureLoaderUtility.BOYKGUN;
            } else if ("Cutie".equals(type)) {
                imgId = ravex.utility.render.TextureLoaderUtility.CUTIE;
            } else if ("Kiss".equals(type)) {
                imgId = ravex.utility.render.TextureLoaderUtility.KISS;
            } else if ("Laying".equals(type)) {
                imgId = ravex.utility.render.TextureLoaderUtility.LAYING;
            } else if ("Licking".equals(type)) {
                imgId = ravex.utility.render.TextureLoaderUtility.LICKING;
            } else if ("Pillow".equals(type)) {
                imgId = ravex.utility.render.TextureLoaderUtility.PILLOW;
            } else if ("Cutieeee".equals(type)) {
                imgId = ravex.utility.render.TextureLoaderUtility.CUTIEEEE;
            } else if ("Cutiemonster".equals(type)) {
                imgId = ravex.utility.render.TextureLoaderUtility.CUTIEMONSTER;
            } else if ("Furik".equals(type)) {
                imgId = ravex.utility.render.TextureLoaderUtility.FURIK;
            } else if ("Godofcoding".equals(type)) {
                imgId = ravex.utility.render.TextureLoaderUtility.GODOFCODING;
            } else if ("Terrydavis".equals(type)) {
                imgId = ravex.utility.render.TextureLoaderUtility.TERRYDAVIS;
            }

            if (imgId != null) {
                int imgW = 120;
                int imgH = 120;
                int imgX = this.width - imgW - 10;
                int imgY = this.height - imgH - 45;

                graphics.blit(imgId, imgX, imgY, imgX + imgW, imgY + imgH, 0.0f, 1.0f, 0.0f, 1.0f);
            }
        }

        if (closing) {
            float tbElapsed = System.currentTimeMillis() - closingStartTime;
            float tbProgress = Math.min(1f, tbElapsed / 130f);
            toolbarAnim = 1f - AnimationUtility.Easing.QUINT_IN.apply(tbProgress);
        } else {
            float tbTarget = ModuleManager.get(ravex.modules.client.ClickGui.class).showToolbar ? 1f : 0f;
            float tbSpeed = tbTarget > toolbarAnim ? 0.15f : 0.25f;
            toolbarAnim += (tbTarget - toolbarAnim) * tbSpeed;
            if (Math.abs(tbTarget - toolbarAnim) < 0.004f) toolbarAnim = tbTarget;
        }

        if (toolbarAnim > 0.01f) {
            int[] tb = getToolbarLayout();
            int mgX = tb[0];
            int mgY = tb[1] + (int) ((1f - toolbarAnim) * -22f);
            int mgW = tb[2];
            int mgH = tb[3];
            int mgGap = tb[4];
            String[] labArr = { ravex.utility.misc.LanguageUtility.t("gui_macros"), ravex.utility.misc.LanguageUtility.t("gui_profiles"), ravex.utility.misc.LanguageUtility.t("gui_configs"), ravex.utility.misc.LanguageUtility.t("gui_reset"), ravex.utility.misc.LanguageUtility.t("gui_hud") };
            boolean tbLive = toolbarAnim > 0.97f;

            macrosHovered      = tbLive && mouseX >= mgX && mouseX <= mgX + mgW && mouseY >= mgY && mouseY <= mgY + mgH;
            profilesHovered    = tbLive && mouseX >= mgX + mgW + mgGap && mouseX <= mgX + 2 * mgW + mgGap && mouseY >= mgY && mouseY <= mgY + mgH;
            configsHovered     = tbLive && mouseX >= mgX + 2 * (mgW + mgGap) && mouseX <= mgX + 3 * mgW + 2 * mgGap && mouseY >= mgY && mouseY <= mgY + mgH;
            resetLayoutHovered = tbLive && mouseX >= mgX + 3 * (mgW + mgGap) && mouseX <= mgX + 4 * mgW + 3 * mgGap && mouseY >= mgY && mouseY <= mgY + mgH;
            hudEditorHovered   = tbLive && mouseX >= mgX + 4 * (mgW + mgGap) && mouseX <= mgX + 5 * mgW + 4 * mgGap && mouseY >= mgY && mouseY <= mgY + mgH;

            int[] bxArr   = { mgX, mgX + mgW + mgGap, mgX + 2 * (mgW + mgGap), mgX + 3 * (mgW + mgGap), mgX + 4 * (mgW + mgGap) };
            boolean[] hovArr = { macrosHovered, profilesHovered, configsHovered, resetLayoutHovered, hudEditorHovered };

            int btnR = Math.min(8, mgH / 2);
            for (int i = 0; i < 5; i++) {
                int bx  = bxArr[i];
                boolean h = hovArr[i];

                int bg = h ? ColorUtility.withAlpha(cachedActiveColor, 40) : 0x18000000;
                Render2DUtility.drawRound(graphics, bx, mgY, mgW, mgH, btnR, ColorUtility.applyAlpha(bg, toolbarAnim));
                if (h) {
                    Render2DUtility.drawRound(graphics, bx, mgY, mgW, mgH, btnR, ColorUtility.applyAlpha(ColorUtility.withAlpha(cachedActiveColor, 20), toolbarAnim));
                }

                int textW = FontRenderUtility.getStringWidth(labArr[i]);
                int textY = mgY + (mgH - FontRenderUtility.getFontHeight()) / 2;
                int textX = bx + (mgW - textW) / 2;
                FontRenderUtility.drawString(graphics, labArr[i], textX, textY,
                        ColorUtility.applyAlpha(h ? 0xFFFFFFFF : 0xFF808090, toolbarAnim), false);
            }
        }

        hoveredDescription = null;

        float finalScale = getAdaptiveScale();
        if (closing && (System.currentTimeMillis() - closingStartTime >= 150)) {
            for (ravex.modules.Module m : ravex.manager.ModuleManager.INSTANCE.getModules()) {
                m.setGearAngle(0f, System.currentTimeMillis());
            }
            ScreenUtility.closeScreen(ravex.mcwrapper.MinecraftWrapper.getWrapper());
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
            descPanelAnim = 1f - AnimationUtility.Easing.QUINT_IN.apply(progress);
            float bindProgress = Math.min(1f, elapsed / 150f);
            bindReveal = 1f - AnimationUtility.Easing.QUINT_IN.apply(bindProgress);
        } else {
            float elapsed = System.currentTimeMillis() - initTime;
            float progress = Math.min(1f, elapsed / 400f);
            searchBarOpenAnim = AnimationUtility.Easing.ELASTIC_OUT.apply(progress);
            float dpProgress = Math.min(1f, elapsed / 250f);
            descPanelAnim = dpProgress;
            float bindProgress = Math.min(1f, Math.max(0f, (elapsed - 150f) / 350f));
            bindReveal = AnimationUtility.Easing.CUBIC_OUT.apply(bindProgress);
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

        renderBeginnerTips(graphics);

        if (ModuleManager.get(ravex.modules.client.ClickGui.class).descriptionPanel) {
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

                int descY = getToolbarY() + getToolbarHeight() + 8;
                if (descY + th > this.height - 4) descY = this.height - 4 - th;
                int descX = (this.width - tw) / 2;

                int da = (int)(descPanelAnim * 255);

                int ly = descY + padY;
                for (String line : lines) {
                    FontRenderUtility.drawString(graphics, line, descX + padX, ly, ColorUtility.setAlpha(0xE0E0E0, da), true);
                    ly += lineH;
                }
            }
        } else {
            if (hoveredDescription != null) {
                activeTooltipText = hoveredDescription;
                float speed = (float) ModuleManager.get(ravex.modules.client.ClickGui.class).tooltipSpeed / 10f;
                tooltipAlpha = Math.min(1.0f, tooltipAlpha + 0.10f * speed);
            } else {
                float speed = (float) ModuleManager.get(ravex.modules.client.ClickGui.class).tooltipSpeed / 10f;
                tooltipAlpha = Math.max(0.0f, tooltipAlpha - 0.15f * speed);
            }

            if (tooltipAlpha > 0.02f && !activeTooltipText.isEmpty()) {
                int ox = (int) ModuleManager.get(ravex.modules.client.ClickGui.class).tooltipOffsetX;
                int oy = (int) ModuleManager.get(ravex.modules.client.ClickGui.class).tooltipOffsetY;
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

                int to = (int) ModuleManager.get(ravex.modules.client.ClickGui.class).descriptionOpacity;
                int ba = (int)(tooltipAlpha * to);
                int ta = (int)(tooltipAlpha * 255);




                int ly = ty + padY;
                for (String line : lines) {
                    FontRenderUtility.drawString(graphics, line, tx + padX, ly, ColorUtility.setAlpha(0xE0E0E0, ta), true);
                    ly += lineH;
                }
            }
        }

        if (activeColorPalette != null) {
            activeColorPalette.render(graphics, originalMouseX, originalMouseY, this.width, this.height);
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    private void renderBeginnerTips(GuiGraphics graphics) {
        boolean show = ModuleManager.get(ravex.modules.client.ClickGui.class).showTips;
        float target = (show && !closing) ? 1f : 0f;
        tipsAnim += (target - tipsAnim) * 0.12f;
        if (Math.abs(target - tipsAnim) < 0.004f) tipsAnim = target;
        if (tipsAnim < 0.02f) return;

        boolean showGear = ModuleManager.get(ravex.modules.client.ClickGui.class).showGear;
        float gearTarget = showGear ? 1f : 0f;
        gearTipAnim += (gearTarget - gearTipAnim) * 0.12f;
        if (Math.abs(gearTarget - gearTipAnim) < 0.004f) gearTipAnim = gearTarget;

        String title = ravex.utility.misc.LanguageUtility.t("gui_tips_title");
        String toggleTip = ravex.utility.misc.LanguageUtility.t("gui_tip_toggle");
        String settingsTip = ravex.utility.misc.LanguageUtility.t("gui_tip_settings");
        String bindTip = ravex.utility.misc.LanguageUtility.t("gui_tip_bind");
        String gearTip = ravex.utility.misc.LanguageUtility.t("gui_tip_gear");

        int lineH = FontRenderUtility.getFontHeight() + 3;
        int padX = 6;
        int padY = 6;
        int titleH = FontRenderUtility.getFontHeight() + 4;
        int maxW = FontRenderUtility.getStringWidth(title);
        maxW = Math.max(maxW, FontRenderUtility.getStringWidth(toggleTip));
        maxW = Math.max(maxW, FontRenderUtility.getStringWidth(settingsTip));
        maxW = Math.max(maxW, FontRenderUtility.getStringWidth(bindTip));
        if (gearTipAnim > 0.02f) {
            maxW = Math.max(maxW, FontRenderUtility.getStringWidth(gearTip));
        }
        int panelH = Math.round(padY + titleH + 3 * lineH + lineH * gearTipAnim + padY);
        int px = 6;
        int py = this.height - panelH - 6;
        if (px < 0) px = 0;
        if (py < 0) py = 0;

        int a = (int)(tipsAnim * 255);
        int titleCol = ColorUtility.withAlpha(0xFFFFFFFF, a);
        int textCol = ColorUtility.withAlpha(0xFFE8E8F0, a);
        int ly = py + padY;
        FontRenderUtility.drawString(graphics, title, px + padX, ly, titleCol, true);
        ly += titleH;
        FontRenderUtility.drawString(graphics, toggleTip, px + padX, ly, textCol, true);
        ly += lineH;
        FontRenderUtility.drawString(graphics, settingsTip, px + padX, ly, textCol, true);
        ly += lineH;
        FontRenderUtility.drawString(graphics, bindTip, px + padX, ly, textCol, true);
        ly += lineH;
        if (gearTipAnim > 0.02f) {
            int gearA = (int)(tipsAnim * gearTipAnim * 255);
            int gearCol = ColorUtility.withAlpha(0xFFE8E8F0, gearA);
            FontRenderUtility.drawString(graphics, gearTip, px + padX, ly, gearCol, true);
        }
    }

    private void renderSearchBar(GuiGraphics graphics, int mouseX, int mouseY) {
        float openAnim = searchBarOpenAnim;
        if (openAnim <= 0.01f) return;
        float openA = Math.min(1f, Math.max(0f, openAnim));

        int barH = getSearchBarHeight();
        int barY = getSearchBarY();
        int barW = Math.min(200, this.width - 60);
        int barX = (this.width - barW) / 2;

        float target = searchFocused ? 1.0f : 0.0f;
        searchAnimProgress += (target - searchAnimProgress) * 0.14f;
        if (Math.abs(target - searchAnimProgress) < 0.004f) searchAnimProgress = target;
        float focus = searchAnimProgress;
        float focusEase = focus * focus * (3f - 2f * focus);
        searchCursorCounter++;

        int pAlpha = (int) ModuleManager.get(ravex.modules.client.ClickGui.class).panelOpacity;
        int active = ColorUtility.getActiveColor();
        int bgAlpha = (int)(AnimationUtility.lerp(pAlpha, Math.min(pAlpha + 20, 255), focusEase) * openA);
        int barBg = ColorUtility.withAlpha(ColorUtility.PANEL_BODY_END, bgAlpha);
        Render2DUtility.drawPixelPerfectRound(graphics, barX, barY, barW, barH, 10, barBg);

        int borderA = (int) (AnimationUtility.lerp(70, 255, focusEase) * openA);
        Render2DUtility.drawPixelPerfectRoundBorder(graphics, barX - 1, barY - 1, barW + 2, barH + 2, 11, 1, ColorUtility.withAlpha(active, borderA));

        int iconSize = 14;
        Identifier searchTex = ravex.utility.render.TextureLoaderUtility.getSearchWhiteTexture();
        if (searchTex != null) {
            int iconX = barX + 8;
            int iconY = barY + (barH - iconSize) / 2;
            int iconColor = ColorUtility.withAlpha(0xFFFFFF, (int) (AnimationUtility.lerp(140, 255, focusEase) * openA));
            graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, searchTex,
                iconX, iconY, 0f, 0f, iconSize, iconSize, iconSize, iconSize, iconColor);
        }

        int textOffset = 8 + iconSize + 4;
        String searchText = searchQuery;
        int textY = barY + (barH - FontRenderUtility.getFontHeight()) / 2 + 1;
        int textColor = ColorUtility.interpolate(0xFF606080, 0xFFD0D0E0, focusEase);
        int placeholderColor = ColorUtility.withAlpha(0xFF606080, (int) ((1f - focusEase * 0.65f) * 200 * openA));
        int maxTextW = barW - textOffset - 40;
        if (searchText.isEmpty()) {
            FontRenderUtility.drawString(graphics, ravex.utility.misc.LanguageUtility.t("gui_search_placeholder"), barX + textOffset, textY, placeholderColor, true);
        } else {
            String clipped = clipToWidth(searchText, maxTextW);
            FontRenderUtility.drawString(graphics, clipped, barX + textOffset, textY, textColor, true);
            if (searchFocused) {
                int textW = FontRenderUtility.getStringWidth(clipped);
                boolean cursorOn = (searchCursorCounter / 30) % 2 == 0;
                float cursorPulse = cursorOn ? 1f : 0.25f;
                int cursorAlpha = (int) (0xD0 * cursorPulse * openA);
                graphics.fill(barX + textOffset + textW, textY - 1, barX + textOffset + 2 + textW, textY + FontRenderUtility.getFontHeight() + 1,
                    ColorUtility.setAlpha(0xFFFFFF, cursorAlpha));
            }
        }

        int underlinePad = 8;
        float underlineBase = Math.max(focusEase, searchQuery.isEmpty() ? 0f : 0.35f);
        float underlineW = (barW - underlinePad * 2) * underlineBase;
        if (underlineW > 1f) {
            int underA = (int) (160 * underlineBase * openA);
            graphics.fill(barX + underlinePad, barY + barH - 2, barX + underlinePad + (int) underlineW, barY + barH - 1,
                ColorUtility.withAlpha(active, underA));
        }

        int resultCount = 0;
        if (!searchQuery.isEmpty()) {
            for (var panel : panels) {
                resultCount += panel.getMatchCount(searchQuery);
            }
        }
        float resultTarget = (!searchQuery.isEmpty() && resultCount > 0) ? 1.0f : 0.0f;
        searchResultAnim += (resultTarget - searchResultAnim) * 0.12f;
        if (Math.abs(resultTarget - searchResultAnim) < 0.004f) searchResultAnim = resultTarget;
        if (searchResultAnim > 0.01f) {
            String countText = String.valueOf(resultCount);
            int cw = FontRenderUtility.getStringWidth(countText);
            int ra = (int) (searchResultAnim * 200 * openA);
            int countColor = ColorUtility.withAlpha(ColorUtility.interpolate(0xFFA0A0C0, ColorUtility.setAlpha(active, 255), searchResultAnim), ra);
            FontRenderUtility.drawString(graphics, countText, barX + barW - cw - 8, textY, countColor, true);
            int sepA = (int) (searchResultAnim * 140 * openA);
            graphics.fill(barX + barW - cw - 11, textY - 1, barX + barW - cw - 9, textY + FontRenderUtility.getFontHeight() + 1,
                ColorUtility.withAlpha(active, sepA));
        }
    }

    private static String clipToWidth(String s, int maxWidth) {
        if (s == null || s.isEmpty() || maxWidth <= 0) return s == null ? "" : s;
        if (FontRenderUtility.getStringWidth(s) <= maxWidth) return s;
        int ellipsisW = FontRenderUtility.getStringWidth("...");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            String next = sb.toString() + s.charAt(i);
            if (FontRenderUtility.getStringWidth(next) + ellipsisW > maxWidth) break;
            sb.append(s.charAt(i));
        }
        return sb + "...";
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
        int barH = getSearchBarHeight();
        int barY = getSearchBarY();

        if (event.x() >= barX && event.x() <= barX + barW && event.y() >= barY && event.y() <= barY + barH) {
            searchFocused = true;
            return true;
        }

        if (bindingModuleButton != null) {
            return super.mouseClicked(event, handled);
        }

        if (toolbarAnim > 0.97f) {
            int[] tb = getToolbarLayout();
            int mgX = tb[0];
            int mgY = tb[1];
            int mgW = tb[2];
            int mgH = tb[3];
            int mgGap = tb[4];

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
                LayoutManager.INSTANCE.reset();
                for (CategoryPanel p : panels) {
                    p.setCustomPosition(false);
                }
                init();
                return true;
            }
            if (event.x() >= mgX + 4 * (mgW + mgGap) && event.x() <= mgX + 5 * mgW + 4 * mgGap && event.y() >= mgY && event.y() <= mgY + mgH) {
                this.minecraft.setScreen(new ravex.gui.hudeditor.HudEditorScreen(this));
                return true;
            }
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
                ravex.RaveX.suppressKey(GLFW.GLFW_KEY_ESCAPE);
            } else {
                bindingModuleButton.getModule().setKeyBind(key);
                ravex.RaveX.suppressKey(key);
                bindingModuleButton.triggerFlash(0.45f);
            }
            bindingModuleButton = null;
            return true;
        }

        if (activeKeybindElement != null) {
            ravex.parameter.KeybindParameter kp = (ravex.parameter.KeybindParameter) activeKeybindElement.getParameter();
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                kp.setValue(org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN);
                ravex.RaveX.suppressKey(GLFW.GLFW_KEY_ESCAPE);
            } else {
                kp.setValue(key);
                ravex.RaveX.suppressKey(key);
            }
            activeKeybindElement = null;
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

    public void saveLayout() {
        Map<String, CategoryPanel> panelMap = new HashMap<>();
        for (CategoryPanel p : panels) {
            panelMap.put(p.getCategory(), p);
        }
        LayoutManager.INSTANCE.save(panelMap, this.width, this.height, 1.0f);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        saveLayout();
        return super.mouseReleased(event);
    }

    @Override
    public void onClose() {
        isDraggingSlider = false;
        saveLayout();
        ModuleButton.expandedModules.clear();
        for (CategoryPanel p : panels) {
            p.resetExpansion();
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
        saveLayout();
        if (activeNumberParameterElement != null) {
            activeNumberParameterElement.applyInput();
            activeNumberParameterElement = null;
        }
        for (CategoryPanel p : panels) {
            p.resetExpansion();
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

        var gp = ModuleManager.get(ravex.modules.client.GuiParticles.class);
        int count = (int) gp.amount;
        count = Math.min(count, 120);
        String pType = gp.type;
        int pColor = gp.color;
        float pSize = (float) gp.size;
        float pSpeed = (float) gp.speed;

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

        Identifier particleTex = ravex.utility.render.TextureLoaderUtility.getParticleTexture(pType, pColor);

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
