package ravex.gui.clickgui;
import ravex.utility.render.ColorUtility;
import ravex.manager.ModuleManager;
import ravex.modules.client.ClickGui;

import net.minecraft.client.gui.GuiGraphics;
import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.client.renderer.RenderPipelines;
import ravex.parameter.ColorParameter;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.PaletteTextureUtility;
import ravex.utility.render.Render2DUtility;
import ravex.utility.render.TextureLoaderUtility;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ColorPaletteModal {
    private final ColorParameter parameter;

    private float hue = 0.0f;
    private float saturation = 1.0f;
    private float value = 1.0f;
    private float alpha = 1.0f;

    private boolean draggingSV = false;
    private boolean draggingHue = false;
    private boolean draggingAlpha = false;

    private static final List<Integer> recentColors = new ArrayList<>();
    private static final int MAX_RECENT = 10;

    private boolean editingHex = false;
    private String hexInput = "";
    private boolean open = true;
    private Runnable onClose = null;
    private float openAnim = 0f;
    private long lastFrameTime = 0L;
    private boolean closing = false;
    private long closingStart = 0L;
    private float syncToggleAnim = 0f;
    private long syncAnimLast = 0L;
    private float rainbowToggleAnim = 0f;
    private long rainbowAnimLast = 0L;

    public boolean isOpen() { return open; }
    public boolean isClosing() { return closing; }

    private final int modalWidth = 220;
    private final int modalHeight = 305;

    private final int svSize = 120;
    private final int sliderHeight = 10;
    private final int swatchSize = 14;
    private final int swatchGap = 4;

    public ColorPaletteModal(ColorParameter parameter) {
        this.parameter = parameter;
        setFromArgb(parameter.getStoredValue());
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    private void setFromArgb(int argb) {
        alpha = ((argb >>> 24) & 0xFF) / 255.0f;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8)  & 0xFF;
        int b =  argb         & 0xFF;
        float[] hsv = java.awt.Color.RGBtoHSB(r, g, b, null);
        hue = hsv[0];
        saturation = hsv[1];
        value = hsv[2];
    }

    public int getArgb() {
        int rgb = hsbToRgb(hue, saturation, value) & 0x00FFFFFF;
        int a = Math.round(alpha * 255) << 24;
        return a | rgb;
    }

    private ClickGui clickGui() {
        return ModuleManager.get(ClickGui.class);
    }

    private boolean isThemeColor() {
        ClickGui cfg = clickGui();
        return cfg != null && parameter.isBoundTo(cfg, "color1");
    }

    private void commitColor(int argb) {
        parameter.setThemeSync(false);
        parameter.setRainbow(false);
        parameter.setValue(argb);
        if (isThemeColor()) {
            ClickGui cfg = clickGui();
            if (cfg != null) cfg.colorMode = "Positive";
        }
        ColorUtility.invalidateColorCache();
    }

    private static int hsbToRgb(float hue, float saturation, float brightness) {
        return java.awt.Color.HSBtoRGB(hue, saturation, brightness);
    }

    private static void addRecentColor(int argb) {
        recentColors.remove((Integer) argb);
        recentColors.add(0, argb);
        while (recentColors.size() > MAX_RECENT) {
            recentColors.remove(recentColors.size() - 1);
        }
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, int screenWidth, int screenHeight) {
        long now = System.currentTimeMillis();
        if (lastFrameTime == 0L) lastFrameTime = now;
        float dt = Math.min(50f, now - lastFrameTime) / 160f;
        lastFrameTime = now;
        float target = closing ? 0f : 1f;
        if (openAnim < target) openAnim = Math.min(target, openAnim + dt);
        else if (openAnim > target) openAnim = Math.max(target, openAnim - dt);
        if (closing && now - closingStart >= 160) {
            close();
            return;
        }
        float ease = 1f - (1f - openAnim) * (1f - openAnim) * (1f - openAnim);
        float scale = 0.88f + 0.12f * ease;
        int mx = (screenWidth - modalWidth) / 2;
        int my = (screenHeight - modalHeight) / 2 + (int) ((1f - ease) * 12);

        graphics.pose().pushMatrix();
        float pivotX = mx + modalWidth / 2f;
        float pivotY = my + modalHeight / 2f;
        graphics.pose().translate(pivotX, pivotY);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-pivotX, -pivotY);

        long win = MinecraftWrapper.getWrapper().getWindow().handle();
        boolean lmb = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (!lmb) {
            draggingSV = draggingHue = draggingAlpha = false;
        }

        int svX = mx + 15;
        int svY = my + 35;
        if (draggingSV) {
            float relX = Math.max(0, Math.min(svSize, mouseX - svX)) / (float) svSize;
            float relY = Math.max(0, Math.min(svSize, mouseY - svY)) / (float) svSize;
            saturation = relX;
            value = 1.0f - relY;
            commitColor(getArgb());
        }

        int hueX = mx + 15;
        int hueY = my + 165;
        int hueW = svSize;
        if (draggingHue) {
            float relX = Math.max(0, Math.min(hueW, mouseX - hueX)) / (float) hueW;
            hue = relX;
            commitColor(getArgb());
        }

        int alphaX = mx + 15;
        int alphaY = my + 183;
        int alphaW = svSize;
        if (draggingAlpha) {
            float relX = Math.max(0, Math.min(alphaW, mouseX - alphaX)) / (float) alphaW;
            alpha = relX;
            commitColor(getArgb());
        }


        Render2DUtility.drawRound(graphics, mx, my, modalWidth, modalHeight, 8, 0xCC0D0D14);
        Render2DUtility.drawSmoothRoundOutline(graphics, mx, my, modalWidth, modalHeight, 8, 1, 0xFF1C1C2A);

        int activeColor = getArgb() | 0xFF000000;


        FontRenderUtility.drawString(graphics, ravex.utility.misc.LanguageUtility.t("pal_title"), mx + 12, my + 10, 0xFFE5E5F0, false);

        graphics.fill(mx + 10, my + 23, mx + modalWidth - 10, my + 24, 0xFF252535);


        graphics.blit(RenderPipelines.GUI_TEXTURED, PaletteTextureUtility.getSvGradient(hue), svX, svY, 0f, 0f, svSize, svSize, PaletteTextureUtility.SIZE, PaletteTextureUtility.SIZE, PaletteTextureUtility.SIZE, PaletteTextureUtility.SIZE, 0xFFFFFFFF);

        Render2DUtility.drawBorder(graphics, svX - 1, svY - 1, svSize + 2, svSize + 2, 1, 0xFF353545);

        int curX = svX + (int)(saturation * svSize);
        int curY = svY + (int)((1.0f - value) * svSize);


        Render2DUtility.fillCircle(graphics, curX, curY, 4, 0xFF000000);
        Render2DUtility.fillCircle(graphics, curX, curY, 3, 0xFFFFFFFF);


        graphics.blit(RenderPipelines.GUI_TEXTURED, PaletteTextureUtility.getHueGradient(), hueX, hueY, 0f, 0f, hueW, sliderHeight, PaletteTextureUtility.SIZE, 1, PaletteTextureUtility.SIZE, 1, 0xFFFFFFFF);

        Render2DUtility.drawBorder(graphics, hueX - 1, hueY - 1, hueW + 2, sliderHeight + 2, 1, 0xFF353545);

        int hkX = hueX + (int)(hue * hueW);
        graphics.fill(hkX - 1, hueY - 2, hkX + 2, hueY + sliderHeight + 2, 0xFFFFFFFF);
        graphics.fill(hkX - 2, hueY - 2, hkX - 1, hueY + sliderHeight + 2, 0xFF000000);
        graphics.fill(hkX + 2, hueY - 2, hkX + 3, hueY + sliderHeight + 2, 0xFF000000);


        int cellW = 10;
        for (int px = 0; px < alphaW; px += cellW) {
            boolean light = (px / cellW) % 2 == 0;
            int chk = light ? 0xFF2A2A3A : 0xFF1A1A25;
            graphics.fill(alphaX + px, alphaY, alphaX + Math.min(px + cellW, alphaW), alphaY + sliderHeight, chk);
        }

        int currentRgb = hsbToRgb(hue, saturation, value) & 0x00FFFFFF;
        graphics.blit(RenderPipelines.GUI_TEXTURED, PaletteTextureUtility.getAlphaGradient(currentRgb), alphaX, alphaY, 0f, 0f, alphaW, sliderHeight, PaletteTextureUtility.SIZE, 1, PaletteTextureUtility.SIZE, 1, 0xFFFFFFFF);

        Render2DUtility.drawBorder(graphics, alphaX - 1, alphaY - 1, alphaW + 2, sliderHeight + 2, 1, 0xFF353545);

        int akX = alphaX + (int)(alpha * alphaW);
        graphics.fill(akX - 1, alphaY - 2, akX + 2, alphaY + sliderHeight + 2, 0xFFFFFFFF);
        graphics.fill(akX - 2, alphaY - 2, akX - 1, alphaY + sliderHeight + 2, 0xFF000000);
        graphics.fill(akX + 2, alphaY - 2, akX + 3, alphaY + sliderHeight + 2, 0xFF000000);


        int previewRadius = 20;
        int previewCX = mx + 175;
        int previewCY = my + 55;
        int previewSize = previewRadius * 2;
        int previewX = previewCX - previewRadius;
        int previewY = previewCY - previewRadius;

        int checkerCell = 4;
        for (int py = 0; py < previewSize; py += checkerCell) {
            for (int px = 0; px < previewSize; px += checkerCell) {
                boolean light = ((px / checkerCell) + (py / checkerCell)) % 2 == 0;
                int chk = light ? 0xFF888888 : 0xFF444444;
                graphics.fill(previewX + px, previewY + py, previewX + Math.min(px + checkerCell, previewSize), previewY + Math.min(py + checkerCell, previewSize), chk);
            }
        }

        Render2DUtility.drawRound(graphics, previewX, previewY, previewSize, previewSize, previewRadius, getArgb());
        Render2DUtility.drawRoundCutout(graphics, previewX, previewY, previewSize, previewSize, previewRadius, 0xFF0D0D14);
        Render2DUtility.drawSmoothRoundOutline(graphics, previewX, previewY, previewSize, previewSize, previewRadius, 1, 0xFF4A4A5A);


        String hex = editingHex ? hexInput : String.format("%08X", getArgb() & 0xFFFFFFFF);
        int hw = FontRenderUtility.getStringWidth(hex);
        int hexX = previewCX - hw / 2;
        int hexY = my + 82;
        FontRenderUtility.drawString(graphics, hex, hexX, hexY, editingHex ? 0xFF5599FF : 0xFFAAAAAA, false);

        if (editingHex) {
            graphics.fill(hexX - 1, hexY + FontRenderUtility.getFontHeight() + 1,
                          hexX + hw + 1, hexY + FontRenderUtility.getFontHeight() + 3, 0xFF5599FF);
        }


        int sectionX = mx + 15;
        int recentY = my + 200;
            FontRenderUtility.drawString(graphics, ravex.utility.misc.LanguageUtility.t("pal_recent"), sectionX, recentY, 0xFF75758A, false);

        int recentSwatchY = recentY + 12;
        for (int i = 0; i < Math.min(recentColors.size(), 10); i++) {
            int col = i % 10;
            int row = i / 10;
            int px = sectionX + (col * (swatchSize + swatchGap));
            int py = recentSwatchY + (row * (swatchSize + swatchGap));
            boolean hovered = mouseX >= px && mouseX <= px + swatchSize && mouseY >= py && mouseY <= py + swatchSize;
            int swCell = 2;
            for (int cy = 0; cy < swatchSize; cy += swCell) {
                for (int cx = 0; cx < swatchSize; cx += swCell) {
                    boolean light = ((cx / swCell) + (cy / swCell)) % 2 == 0;
                    int chk = light ? 0xFF888888 : 0xFF444444;
                    graphics.fill(px + cx, py + cy, px + Math.min(cx + swCell, swatchSize), py + Math.min(cy + swCell, swatchSize), chk);
                }
            }
            graphics.fill(px, py, px + swatchSize, py + swatchSize, recentColors.get(i));
            Render2DUtility.drawBorder(graphics, px, py, swatchSize, swatchSize, 1, hovered ? 0x99FFFFFF : 0xFF2A2A3A);
        }

        int presetY = my + 232;
            FontRenderUtility.drawString(graphics, ravex.utility.misc.LanguageUtility.t("pal_presets"), sectionX, presetY, 0xFF75758A, false);

        int[] presets = {
            0xFFFFFFFF, 0xFFC0C0C0, 0xFF808080, 0xFF000000,
            0xFFFF5555, 0xFFFF8800, 0xFFFFAA00, 0xFFFFFF55,
            0xFF55FF55, 0xFF00CC88, 0xFF5555FF, 0xFF8844FF,
            0xFF55FFFF, 0xFF00AAAA, 0xFFFF55FF, 0xFFFF4488
        };

        int presetSwatchY = presetY + 12;
        for (int i = 0; i < presets.length; i++) {
            int col = i % 8;
            int row = i / 8;
            int px = sectionX + (col * (swatchSize + swatchGap));
            int py = presetSwatchY + (row * (swatchSize + swatchGap));
            boolean hovered = mouseX >= px && mouseX <= px + swatchSize && mouseY >= py && mouseY <= py + swatchSize;
            graphics.fill(px, py, px + swatchSize, py + swatchSize, presets[i]);
            int borderColor = hovered ? 0xFFFFFFFF : 0xFF2A2A3A;
            Render2DUtility.drawBorder(graphics, px - 1, py - 1, swatchSize + 2, swatchSize + 2, 1, borderColor);
        }


        int iconSize = 16;
        int btnY = my + modalHeight - 24;
        int cancelX = mx + 15;
        int applyX = mx + modalWidth - iconSize - 15;

        boolean cancelHovered = mouseX >= cancelX - 2 && mouseX <= cancelX + iconSize + 2 && mouseY >= btnY - 2 && mouseY <= btnY + iconSize + 2;
        boolean applyHovered = mouseX >= applyX - 2 && mouseX <= applyX + iconSize + 2 && mouseY >= btnY - 2 && mouseY <= btnY + iconSize + 2;


        int cancelCol = cancelHovered ? 0xDDFFFFFF : 0x77FFFFFF;
        graphics.pose().pushMatrix();
        graphics.pose().translate(cancelX, btnY);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TextureLoaderUtility.DISABLE, 0, 0, 0f, 0f, iconSize, iconSize, iconSize, iconSize, cancelCol);
        graphics.pose().popMatrix();


        int applyCol = applyHovered ? 0xDDFFFFFF : 0x77FFFFFF;
        graphics.pose().pushMatrix();
        graphics.pose().translate(applyX, btnY);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TextureLoaderUtility.ENABLE, 0, 0, 0f, 0f, iconSize, iconSize, iconSize, iconSize, applyCol);
        graphics.pose().popMatrix();


        long nowTs = System.currentTimeMillis();
        if (syncAnimLast == 0L) syncAnimLast = nowTs;
        float syncDt = Math.min(50f, nowTs - syncAnimLast) / 160f;
        syncAnimLast = nowTs;
        float syncTarget = parameter.isThemeSync() ? 1f : 0f;
        if (syncToggleAnim < syncTarget) syncToggleAnim = Math.min(syncTarget, syncToggleAnim + syncDt);
        else if (syncToggleAnim > syncTarget) syncToggleAnim = Math.max(syncTarget, syncToggleAnim - syncDt);

        int swW = 28;
        int swH = 14;
        int swX = mx + 175 - swW / 2;
        int swY = my + 122;
        boolean syncHovered = mouseX >= swX && mouseX <= swX + swW && mouseY >= swY - 14 && mouseY <= swY + swH + 4;

        int syncLabelY = my + 105;
        String syncLabel = ravex.utility.misc.LanguageUtility.t("pal_theme");
        int labelW = FontRenderUtility.getStringWidth(syncLabel);
        int syncLabelX = mx + 175 - labelW / 2;
        int syncLabelCol = syncHovered
            ? ColorUtility.interpolate(0xFFD0D0E0, 0xFFFFFFFF, syncToggleAnim)
            : ColorUtility.interpolate(0xFF7A7A8A, 0xFFE0E0F0, syncToggleAnim);
        FontRenderUtility.drawString(graphics, syncLabel, syncLabelX, syncLabelY, syncLabelCol, false);

        int trackColor = ColorUtility.interpolate(0xFF2A2A3A, activeColor, syncToggleAnim);
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) swX, (float) swY);
        Render2DUtility.drawRound(graphics, 0, 0, swW, swH, swH / 2, trackColor);
        graphics.pose().popMatrix();

        float knobSize = 12f;
        float knobRange = swW - knobSize - 2;
        float knobDrawX = swX + 1f + syncToggleAnim * knobRange;
        float knobDrawY = swY + 1f;
        net.minecraft.resources.Identifier knobTex = Render2DUtility.getSmoothCircle();
        graphics.pose().pushMatrix();
        graphics.pose().translate(knobDrawX, knobDrawY);
        graphics.blit(RenderPipelines.GUI_TEXTURED, knobTex, 0, 0, 0f, 0f, (int) knobSize, (int) knobSize, (int) knobSize, (int) knobSize, 0xFFFFFFFF);
        graphics.pose().popMatrix();

        boolean themeColor = isThemeColor();
        long nowR = System.currentTimeMillis();
        if (rainbowAnimLast == 0L) rainbowAnimLast = nowR;
        float rdt = Math.min(50f, nowR - rainbowAnimLast) / 160f;
        rainbowAnimLast = nowR;
        ClickGui cfg = clickGui();
        boolean rainbowOn = themeColor
            ? cfg != null && "Rainbow".equals(cfg.colorMode)
            : parameter.isRainbow();
        float rTarget = rainbowOn ? 1f : 0f;
        if (rainbowToggleAnim < rTarget) rainbowToggleAnim = Math.min(rTarget, rainbowToggleAnim + rdt);
        else if (rainbowToggleAnim > rTarget) rainbowToggleAnim = Math.max(rTarget, rainbowToggleAnim - rdt);

        int rSwY = my + 166;
        boolean rHovered = mouseX >= swX && mouseX <= swX + swW && mouseY >= rSwY - 14 && mouseY <= rSwY + swH + 4;

        int rLabelY = my + 150;
        String rLabel = ravex.utility.misc.LanguageUtility.t("pal_rainbow");
        int rLabelW = FontRenderUtility.getStringWidth(rLabel);
        int rLabelX = mx + 175 - rLabelW / 2;
        int rLabelCol = rHovered
            ? ColorUtility.interpolate(0xFFD0D0E0, 0xFFFFFFFF, rainbowToggleAnim)
            : ColorUtility.interpolate(0xFF7A7A8A, 0xFFE0E0F0, rainbowToggleAnim);
        FontRenderUtility.drawString(graphics, rLabel, rLabelX, rLabelY, rLabelCol, false);

        int rBaseColor = themeColor ? ColorUtility.getColorRGB(0) : getArgb();
        int rTrackColor = ColorUtility.interpolate(0xFF2A2A3A, rBaseColor, rainbowToggleAnim);
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) swX, (float) rSwY);
        Render2DUtility.drawRound(graphics, 0, 0, swW, swH, swH / 2, rTrackColor);
        graphics.pose().popMatrix();

        float rKnobX = swX + 1f + rainbowToggleAnim * knobRange;
        graphics.pose().pushMatrix();
        graphics.pose().translate(rKnobX, rSwY + 1f);
        graphics.blit(RenderPipelines.GUI_TEXTURED, knobTex, 0, 0, 0f, 0f, (int) knobSize, (int) knobSize, (int) knobSize, (int) knobSize, 0xFFFFFFFF);
        graphics.pose().popMatrix();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (closing) return true;
        int mx = (int) mouseX;
        int my = (int) mouseY;

        int screenWidth = MinecraftWrapper.getWrapper().getWindow().getGuiScaledWidth();
        int screenHeight = MinecraftWrapper.getWrapper().getWindow().getGuiScaledHeight();
        int mxLeft = (screenWidth - modalWidth) / 2;
        int myTop = (screenHeight - modalHeight) / 2;

        if (mx < mxLeft || mx > mxLeft + modalWidth || my < myTop || my > myTop + modalHeight) {
            editingHex = false;
            requestClose();
            return true;
        }

        int svX = mxLeft + 15;
        int svY = myTop + 35;
        if (mx >= svX && mx < svX + svSize && my >= svY && my < svY + svSize) {
            draggingSV = true;
            saturation = (mx - svX) / (float) svSize;
            value = 1.0f - (my - svY) / (float) svSize;
            commitColor(getArgb());
            return true;
        }

        int hueX = mxLeft + 15;
        int hueY = myTop + 165;
        if (mx >= hueX && mx < hueX + svSize && my >= hueY && my < hueY + sliderHeight) {
            draggingHue = true;
            hue = (mx - hueX) / (float) svSize;
            commitColor(getArgb());
            return true;
        }

        int alphaX = mxLeft + 15;
        int alphaY = myTop + 183;
        if (mx >= alphaX && mx < alphaX + svSize && my >= alphaY && my < alphaY + sliderHeight) {
            draggingAlpha = true;
            alpha = (mx - alphaX) / (float) svSize;
            commitColor(getArgb());
            return true;
        }

        int previewCX = mxLeft + 175;
        String hex = editingHex ? hexInput : String.format("%08X", getArgb() & 0xFFFFFFFF);
        int hw = FontRenderUtility.getStringWidth(hex);
        int hexX = previewCX - hw / 2;
        int hexY = myTop + 82;
        int fh = FontRenderUtility.getFontHeight();
        if (mx >= hexX - 2 && mx <= hexX + hw + 2 && my >= hexY - 2 && my <= hexY + fh + 4) {
            editingHex = !editingHex;
            hexInput = String.format("%08X", getArgb() & 0xFFFFFFFF);
            if (!editingHex) {
                applyHexInput();
            }
            playClickSound();
            return true;
        }

        int swW = 28;
        int swH = 14;
        int swX = mxLeft + 175 - swW / 2;
        int swY = myTop + 122;
        if (mx >= swX && mx <= swX + swW && my >= swY - 14 && my <= swY + swH + 4) {
            parameter.setThemeSync(!parameter.isThemeSync());
            if (parameter.isThemeSync()) {
                parameter.setRainbow(false);
                setFromArgb(ColorUtility.getActiveColor());
                parameter.setValue(ColorUtility.getActiveColor());
            }
            playClickSound();
            return true;
        }

        int rSwW = 28;
        int rSwH = 14;
        int rSwX = mxLeft + 175 - rSwW / 2;
        int rSwY = myTop + 166;
        if (mx >= rSwX && mx <= rSwX + rSwW && my >= rSwY - 14 && my <= rSwY + rSwH + 4) {
            if (isThemeColor()) {
                ClickGui cfg = clickGui();
                if (cfg != null) cfg.colorMode = "Rainbow".equals(cfg.colorMode) ? "Positive" : "Rainbow";
                ColorUtility.invalidateColorCache();
            } else {
                parameter.setRainbow(!parameter.isRainbow());
                if (parameter.isRainbow()) parameter.setThemeSync(false);
            }
            playClickSound();
            return true;
        }

        int sectionX = mxLeft + 15;
        int recentY = myTop + 200;
        int recentSwatchY = recentY + 12;
        for (int i = 0; i < Math.min(recentColors.size(), 10); i++) {
            int col = i % 10;
            int row = i / 10;
            int px = sectionX + (col * (swatchSize + swatchGap));
            int py = recentSwatchY + (row * (swatchSize + swatchGap));
            if (mx >= px && mx <= px + swatchSize && my >= py && my <= py + swatchSize) {
                setFromArgb(recentColors.get(i));
                commitColor(recentColors.get(i));
                editingHex = false;
                playClickSound();
                return true;
            }
        }

        int presetY = myTop + 232;
        int[] presets = {
            0xFFFFFFFF, 0xFFC0C0C0, 0xFF808080, 0xFF000000,
            0xFFFF5555, 0xFFFF8800, 0xFFFFAA00, 0xFFFFFF55,
            0xFF55FF55, 0xFF00CC88, 0xFF5555FF, 0xFF8844FF,
            0xFF55FFFF, 0xFF00AAAA, 0xFFFF55FF, 0xFFFF4488
        };
        int presetSwatchY = presetY + 12;
        for (int i = 0; i < presets.length; i++) {
            int col = i % 8;
            int row = i / 8;
            int px = sectionX + (col * (swatchSize + swatchGap));
            int py = presetSwatchY + (row * (swatchSize + swatchGap));
            if (mx >= px && mx <= px + swatchSize && my >= py && my <= py + swatchSize) {
                setFromArgb(presets[i]);
                commitColor(presets[i]);
                editingHex = false;
                playClickSound();
                return true;
            }
        }

        int iconSize = 16;
        int btnY = myTop + modalHeight - 24;
        int cancelX = mxLeft + 15;
        int applyX = mxLeft + modalWidth - iconSize - 15;


        if (mx >= cancelX - 2 && mx <= cancelX + iconSize + 2 && my >= btnY - 2 && my <= btnY + iconSize + 2) {
            parameter.setValue(parameter.getValue());
            editingHex = false;
            requestClose();
            playClickSound();
            return true;
        }


        if (mx >= applyX - 2 && mx <= applyX + iconSize + 2 && my >= btnY - 2 && my <= btnY + iconSize + 2) {
            editingHex = false;
            addRecentColor(getArgb());
            requestClose();
            playClickSound();
            return true;
        }

        return true;
    }

    public boolean keyPressed(int key) {
        if (closing) return true;
        if (editingHex) {
            if (key == GLFW.GLFW_KEY_ENTER) {
                applyHexInput();
                editingHex = false;
                playClickSound();
                return true;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                editingHex = false;
                return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE && !hexInput.isEmpty()) {
                hexInput = hexInput.substring(0, hexInput.length() - 1);
                return true;
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER) {
            addRecentColor(getArgb());
            requestClose();
            playClickSound();
            return true;
        }
        return true;
    }

    public boolean charTyped(char codePoint) {
        if (editingHex && hexInput.length() < 8) {
            String hexChars = "0123456789abcdefABCDEF";
            if (hexChars.indexOf(codePoint) >= 0) {
                hexInput += Character.toUpperCase(codePoint);
                return true;
            }
        }
        return true;
    }

    private void applyHexInput() {
        if (hexInput.length() != 8) return;
        try {
            int argb = (int) Long.parseLong(hexInput, 16);
            setFromArgb(argb);
            commitColor(argb);
        } catch (NumberFormatException ignored) {}
    }

    private void requestClose() {
        if (closing) return;
        closing = true;
        closingStart = System.currentTimeMillis();
    }

    private void close() {
        open = false;
        ClickGUI.activeColorParameter = null;
        ClickGUI.activeColorPalette = null;
        if (onClose != null) onClose.run();
    }

    private void playClickSound() {
    }
}
