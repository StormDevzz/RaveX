package ravex.gui.onboarding;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import ravex.event.EventBusHolder;
import ravex.event.client.SoundEvent;
import ravex.manager.ConfigManager;
import ravex.manager.ModuleManager;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Module;
import ravex.modules.ModuleProxy;
import ravex.utility.misc.LanguageUtility;
import ravex.utility.misc.ScreenUtility;
import ravex.utility.render.ColorUtility;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DUtility;
import ravex.utility.render.animate.AnimationUtility;
import ravex.utility.sound.SoundUtility;

import java.io.File;
import java.io.InputStream;

public class OnboardingScreen extends Screen {
    private static final File MARKER = new File(".", "RaveX/onboarded");
    private static final Identifier FLAG_US = Identifier.fromNamespaceAndPath("ravex", "textures/flag_us");
    private static final Identifier FLAG_RU = Identifier.fromNamespaceAndPath("ravex", "textures/flag_ru");
    private static final Identifier GREETINGS = Identifier.fromNamespaceAndPath("ravex", "textures/greetings");
    private static final Identifier CHECK = Identifier.fromNamespaceAndPath("ravex", "textures/check");
    private static Boolean markerCache;
    private static boolean texturesLoaded = false;

    private final String initialLang;
    private String selectedLang;

    private int step = 0;
    private int shownStep = 0;
    private int pendingStep = -1;
    private float transition = 1f;

    private float time = 0f;
    private long lastNanos = System.nanoTime();
    private float doneTime = -1f;

    private float continueHover = 0f;
    private float enterHover = 0f;
    private float enHover = 0f;
    private float ruHover = 0f;
    private float enSelect = 0f;
    private float ruSelect = 0f;
    private float langSwap = -1f;
    private float langDip = 1f;
    private boolean contWasHovered = false;
    private boolean enterWasHovered = false;
    private boolean cardWasHovered = false;

    private final int[] contRect = new int[4];
    private final int[] enterRect = new int[4];
    private final int[] enRect = new int[4];
    private final int[] ruRect = new int[4];

    private float uiScale = 1f;
    private int uiX;
    private int uiY;

    public OnboardingScreen() {
        super(Component.literal("RaveX"));
        markCompleted();
        initialLang = LanguageUtility.getLanguage();
        selectedLang = initialLang;
        if ("Russian".equals(initialLang)) {
            enSelect = 0f;
            ruSelect = 1f;
        }
    }

    public static boolean isCompleted() {
        if (markerCache != null) return markerCache;
        markerCache = MARKER.exists();
        return markerCache;
    }

    private static void markCompleted() {
        markerCache = true;
        try {
            File parent = MARKER.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            if (!MARKER.exists()) MARKER.createNewFile();
        } catch (Exception ignored) {
        }
    }

    private static void ensureTextures() {
        if (texturesLoaded) return;
        loadTexture(FLAG_US, "/assets/ravex/textures/flag_us.png", "flag_us");
        loadTexture(FLAG_RU, "/assets/ravex/textures/flag_ru.png", "flag_ru");
        loadTexture(GREETINGS, "/assets/ravex/textures/greetings.png", "greetings");
        loadTexture(CHECK, "/assets/ravex/textures/check.png", "check");
        texturesLoaded = true;
    }

    private static void loadTexture(Identifier id, String path, String name) {
        try (InputStream stream = OnboardingScreen.class.getResourceAsStream(path)) {
            if (stream == null) return;
            NativeImage image = NativeImage.read(stream);
            DynamicTexture tex = new DynamicTexture(() -> name, image);
            Render2DUtility.setLinearSampler(tex);
            MinecraftWrapper.getWrapper().getTextureManager().register(id, tex);
        } catch (Exception ignored) {
        }
    }

    private static void applyLanguage(String lang) {
        LanguageUtility.setLanguage(lang);
        Module m = ModuleManager.delegate(ravex.modules.client.Settings.class);
        if (m instanceof ModuleProxy proxy && proxy.getComponent() instanceof ravex.modules.client.Settings settings) {
            settings.ModeParameter = lang;
        }
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : Math.min(1f, v);
    }

    private static float approach(float cur, float target, float speed, float dt) {
        float next = cur + (target - cur) * Math.min(1f, dt * speed);
        if (Math.abs(next - target) < 0.002f) next = target;
        return next;
    }

    private static int withAlpha(int color, float alpha) {
        int base = (color >>> 24) & 0xFF;
        return (color & 0x00FFFFFF) | ((int) (base * clamp01(alpha)) << 24);
    }

    private static int brighten(int color, int amount) {
        int r = Math.min(255, ((color >> 16) & 0xFF) + amount);
        int g = Math.min(255, ((color >> 8) & 0xFF) + amount);
        int b = Math.min(255, (color & 0xFF) + amount);
        return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    private static int darken(int color, int amount) {
        int r = Math.max(0, ((color >> 16) & 0xFF) - amount);
        int g = Math.max(0, ((color >> 8) & 0xFF) - amount);
        int b = Math.max(0, (color & 0xFF) - amount);
        return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    private static void drawCentered(GuiGraphics g, FontRenderUtility.FontType font, String text, int centerX, int y, int color, boolean shadow) {
        int w = FontRenderUtility.getStringWidth(font, text);
        FontRenderUtility.drawString(g, font, text, centerX - w / 2, y, color, shadow);
    }

    private static void drawFitted(GuiGraphics g, FontRenderUtility.FontType font, String text, int centerX, int y, int maxW, int color) {
        FontRenderUtility.FitText fit = FontRenderUtility.fitText(font, text, maxW);
        FontRenderUtility.drawString(g, font, fit.text, centerX - fit.width / 2, y, color, true);
    }

    private static void drawFlagRu(GuiGraphics g, int x, int y, int w, int h, float af) {
        g.blit(RenderPipelines.GUI_TEXTURED, FLAG_RU, x, y, 0f, 0f, w, h, 160, 107, 160, 107, withAlpha(0xFFFFFFFF, af));
        Render2DUtility.drawBorder(g, x - 1, y - 1, w + 2, h + 2, 1, withAlpha(0x40FFFFFF, af));
    }

    private static void drawFlagUs(GuiGraphics g, int x, int y, int w, int h, float af) {
        g.blit(RenderPipelines.GUI_TEXTURED, FLAG_US, x, y, 0f, 0f, w, h, 160, 84, 160, 84, withAlpha(0xFFFFFFFF, af));
        Render2DUtility.drawBorder(g, x - 1, y - 1, w + 2, h + 2, 1, withAlpha(0x40FFFFFF, af));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void goToStep(int next) {
        if (transitioning() || next == step || next < 0 || next > 2) return;
        shownStep = step;
        pendingStep = next;
        transition = 0f;
        EventBusHolder.get().post(new SoundEvent(SoundEvent.Type.SLIDE, 0.45f));
    }

    private boolean transitioning() {
        return pendingStep >= 0;
    }

    private void advance() {
        if (transitioning()) return;
        if (step == 0) goToStep(1);
        else if (step == 1) goToStep(2);
        else if (step == 2) finish();
    }

    private void finish() {
        applyLanguage(selectedLang);
        try {
            ConfigManager.INSTANCE.save("default");
        } catch (Exception ignored) {
        }
        markCompleted();
        SoundUtility.playOnboardingDone(1.0f);
        ScreenUtility.setToTitle(ravex.mcwrapper.MinecraftWrapper.getWrapper());
    }

    private void skip() {
        applyLanguage(initialLang);
        markCompleted();
        ScreenUtility.setToTitle(ravex.mcwrapper.MinecraftWrapper.getWrapper());
    }

    private void selectLanguage(String lang) {
        if (lang.equals(selectedLang)) return;
        selectedLang = lang;
        langSwap = time;
        applyLanguage(lang);
        EventBusHolder.get().post(new SoundEvent(SoundEvent.Type.TOGGLE, 0.55f));
    }

    private boolean hit(int[] rect, int mx, int my) {
        return mx >= rect[0] && mx <= rect[0] + rect[2] && my >= rect[1] && my <= rect[1] + rect[3];
    }

    private void updateAnims(float dt, int mouseX, int mouseY) {
        if (transitioning()) {
            transition += dt / 0.34f;
            if (transition >= 1f) {
                step = pendingStep;
                pendingStep = -1;
                transition = 1f;
                if (step == 2 && doneTime < 0f) doneTime = time;
            }
        }

        boolean contHovered = mouseX >= contRect[0] && mouseX <= contRect[0] + contRect[2]
            && mouseY >= contRect[1] && mouseY <= contRect[1] + contRect[3];
        boolean enterHovered = mouseX >= enterRect[0] && mouseX <= enterRect[0] + enterRect[2]
            && mouseY >= enterRect[1] && mouseY <= enterRect[1] + enterRect[3];
        boolean enHovered = mouseX >= enRect[0] && mouseX <= enRect[0] + enRect[2]
            && mouseY >= enRect[1] && mouseY <= enRect[1] + enRect[3];
        boolean ruHovered = mouseX >= ruRect[0] && mouseX <= ruRect[0] + ruRect[2]
            && mouseY >= ruRect[1] && mouseY <= ruRect[1] + ruRect[3];

        continueHover = approach(continueHover, (contHovered && step < 2) || (enterHovered && step == 2) ? 1f : 0f, 14f, dt);
        enterHover = approach(enterHover, enterHovered && step == 2 ? 1f : 0f, 14f, dt);
        enHover = approach(enHover, enHovered ? 1f : 0f, 14f, dt);
        ruHover = approach(ruHover, ruHovered ? 1f : 0f, 14f, dt);
        enSelect = approach(enSelect, "English".equals(selectedLang) ? 1f : 0f, 12f, dt);
        ruSelect = approach(ruSelect, "Russian".equals(selectedLang) ? 1f : 0f, 12f, dt);

        boolean anyHover = (contHovered && step < 2) || (enterHovered && step == 2) || enHovered || ruHovered;
        if (anyHover && !contWasHovered && !enterWasHovered && !cardWasHovered) {
            EventBusHolder.get().post(new SoundEvent(SoundEvent.Type.HOVER, 0.45f));
        }
        contWasHovered = contHovered && step < 2;
        enterWasHovered = enterHovered && step == 2;
        cardWasHovered = enHovered || ruHovered;

        if (langSwap >= 0f) {
            float lt = (time - langSwap) / 0.36f;
            if (lt >= 1f) {
                langSwap = -1f;
                langDip = 1f;
            } else {
                langDip = 1f - 0.9f * (float) Math.sin(Math.PI * lt);
            }
        }
    }

    private void layout(int cardX, int cardY, int cardW, int cardH) {
        contRect[0] = cardX + cardW - 16 - 142;
        contRect[1] = cardY + cardH - 16 - 34;
        contRect[2] = 142;
        contRect[3] = 34;

        enterRect[0] = cardX + (cardW - 196) / 2;
        enterRect[1] = cardY + cardH - 16 - 34;
        enterRect[2] = 196;
        enterRect[3] = 34;

        int cardGap = 14;
        int langW = (cardW - 32 - cardGap) / 2;
        enRect[0] = cardX + 16;
        enRect[1] = cardY + 96;
        enRect[2] = langW;
        enRect[3] = 112;
        ruRect[0] = cardX + 16 + langW + cardGap;
        ruRect[1] = cardY + 96;
        ruRect[2] = langW;
        ruRect[3] = 112;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        ensureTextures();

        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;
        time += dt;

        int accent = ColorUtility.getActiveColor();
        // fixed design size, uiScale keeps it stable on any gui scale
        int cardW = 500;
        int cardH = 306;
        float guiScale = MinecraftWrapper.getWrapper().getWindow().getGuiScale();
        if (!(guiScale >= 1f)) guiScale = 1f;
        float ui = 2f / guiScale;
        ui = Math.min(ui, this.width * 0.94f / cardW);
        ui = Math.min(ui, this.height * 0.94f / cardH);
        uiScale = ui;
        uiX = Math.round((this.width - cardW * ui) / 2f);
        uiY = Math.round((this.height - cardH * ui) / 2f);
        layout(0, 0, cardW, cardH);
        int designX = Math.round((mouseX - uiX) / ui);
        int designY = Math.round((mouseY - uiY) / ui);
        updateAnims(dt, designX, designY);

        graphics.fill(0, 0, this.width, this.height, 0x9604050B);
        graphics.pose().pushMatrix();
        graphics.pose().translate(uiX, uiY);
        graphics.pose().scale(ui, ui);
        int cardX = 0;
        int cardY = 0;

        Render2DUtility.drawGaussianShadow(graphics, cardX - 6, cardY - 4, cardW + 12, cardH + 16, 24, 0x77000000);
        Render2DUtility.drawRoundQ(graphics, cardX, cardY, cardW, cardH, 18, 0xF20B0E18);
        Render2DUtility.drawSmoothRoundOutline(graphics, cardX, cardY, cardW, cardH, 18, 1,
            ColorUtility.withAlpha(accent, 110));
        Render2DUtility.drawRoundGradient(graphics, cardX + 18, cardY, cardW - 36, 3, 1,
            ColorUtility.withAlpha(accent, 150), ColorUtility.withAlpha(accent, 40),
            ColorUtility.withAlpha(accent, 0), ColorUtility.withAlpha(accent, 0));

        drawProgressDots(graphics, cardX, cardW, cardY, accent, dt);

        int centerX = cardX + cardW / 2;
        if (transitioning()) {
            float p = AnimationUtility.Easing.CUBIC_OUT.apply(clamp01(transition));
            drawStep(graphics, shownStep, clamp01(1f - p), -56f * p, centerX, cardX, cardY, cardW, cardH, accent);
            drawStep(graphics, pendingStep, clamp01(p), 56f * (1f - p), centerX, cardX, cardY, cardW, cardH, accent);
        } else {
            drawStep(graphics, step, 1f, 0f, centerX, cardX, cardY, cardW, cardH, accent);
        }

        String hint = LanguageUtility.t("ob_skip_hint");
        FontRenderUtility.drawString(graphics, FontRenderUtility.FontType.SF_MEDIUM, hint, cardX, cardY + cardH + 12, 0x88FFFFFF, false);
        String counter = (step + 1) + " / 3";
        int counterW = FontRenderUtility.getStringWidth(FontRenderUtility.FontType.SF_MEDIUM, counter);
        FontRenderUtility.drawString(graphics, FontRenderUtility.FontType.SF_MEDIUM, counter, cardX + cardW - counterW, cardY + cardH + 12, 0x88FFFFFF, false);
        graphics.pose().popMatrix();
    }

    private void drawProgressDots(GuiGraphics graphics, int cardX, int cardW, int cardY, int accent, float dt) {
        int dotY = cardY + 16;
        float[] widths = new float[3];
        int total = 0;
        for (int i = 0; i < 3; i++) {
            float target = i == step ? 24f : 8f;
            widths[i] = approach(widthsCached[i], target, 16f, dt);
            widthsCached[i] = widths[i];
            total += Math.round(widths[i]);
        }
        total += 12;
        int x = cardX + (cardW - total) / 2;
        for (int i = 0; i < 3; i++) {
            int w = Math.round(widths[i]);
            int color = i <= step ? accent : 0x33FFFFFF;
            Render2DUtility.drawRoundQ(graphics, x, dotY, w, 6, 3, color);
            x += w + 6;
        }
    }

    private final float[] widthsCached = {8f, 8f, 8f};

    private void drawStep(GuiGraphics g, int idx, float alpha, float dx, int centerX, int cardX, int cardY, int cardW, int cardH, int accent) {
        int a = (int) (255 * clamp01(alpha));
        if (a <= 0) return;
        int ox = Math.round(dx);

        if (idx == 0) {
            drawWelcome(g, centerX + ox, cardX, cardY, cardW, cardH, accent, a);
        } else if (idx == 1) {
            drawLanguage(g, centerX + ox, cardX, cardY, cardW, cardH, accent, a);
        } else {
            drawDone(g, centerX + ox, cardX, cardY, cardW, cardH, accent, a);
        }
    }

    private void drawWelcome(GuiGraphics g, int centerX, int cardX, int cardY, int cardW, int cardH, int accent, int alpha) {
        float af = alpha / 255f;
        int logoW = 64;
        int logoH = 64;
        int logoX = centerX - logoW / 2;
        int logoY = cardY + 28;
        Render2DUtility.drawGaussianShadow(g, logoX - 8, logoY - 8, logoW + 16, logoH + 16, 20,
            withAlpha(ColorUtility.withAlpha(accent, 100), af));
        g.blit(RenderPipelines.GUI_TEXTURED, GREETINGS, logoX, logoY, 0f, 0f, logoW, logoH, 512, 512, 512, 512,
            withAlpha(0xFFFFFFFF, af));

        drawCentered(g, FontRenderUtility.FontType.SF_BOLD, LanguageUtility.t("ob_welcome_title"),
            centerX, cardY + 116, withAlpha(0xFFFFFFFF, af), true);
        drawFitted(g, FontRenderUtility.FontType.SF_MEDIUM, LanguageUtility.t("ob_welcome_sub"),
            centerX, cardY + 146, cardW - 70, withAlpha(0xFFB8BCD0, af));

        String label = LanguageUtility.t("ob_continue");
        drawPillButton(g, contRect, label, accent, continueHover, alpha, !transitioning());
    }

    private void drawLanguage(GuiGraphics g, int centerX, int cardX, int cardY, int cardW, int cardH, int accent, int alpha) {
        int a2 = Math.round(alpha * langDip);
        drawCentered(g, FontRenderUtility.FontType.SF_BOLD, LanguageUtility.t("ob_lang_title"),
            centerX, cardY + 40, withAlpha(0xFFFFFFFF, a2 / 255f), true);
        drawFitted(g, FontRenderUtility.FontType.SF_MEDIUM, LanguageUtility.t("ob_lang_sub"),
            centerX, cardY + 66, cardW - 70, withAlpha(0xFFA8ACC4, a2 / 255f));

        drawLangCard(g, enRect, LanguageUtility.t("ob_lang_en_name"), LanguageUtility.t("ob_lang_en_sub"),
            accent, enSelect, enHover, a2, true);
        drawLangCard(g, ruRect, LanguageUtility.t("ob_lang_ru_name"), LanguageUtility.t("ob_lang_ru_sub"),
            accent, ruSelect, ruHover, a2, false);

        String label = LanguageUtility.t("ob_continue");
        drawPillButton(g, contRect, label, accent, continueHover, a2, !transitioning());
    }

    private void drawLangCard(GuiGraphics g, int[] rect, String name, String sub, int accent, float selectAnim, float hoverAnim, int alpha, boolean isEn) {
        int x = rect[0];
        int y = rect[1];
        int w = rect[2];
        int h = rect[3];
        float af = alpha / 255f;

        Render2DUtility.drawRoundQ(g, x, y, w, h, 12, withAlpha(0xFF12162A, af));
        if (selectAnim > 0.01f) {
            Render2DUtility.drawRoundQ(g, x, y, w, h, 12, withAlpha(ColorUtility.withAlpha(accent, 40), selectAnim * af));
        }
        int baseBorder = ColorUtility.interpolate(ColorUtility.withAlpha(accent, 70),
            ColorUtility.withAlpha(accent, 150), hoverAnim);
        Render2DUtility.drawSmoothRoundOutline(g, x, y, w, h, 12, 1, withAlpha(baseBorder, af));
        if (selectAnim > 0.01f) {
            Render2DUtility.drawSmoothRoundOutline(g, x, y, w, h, 12, 2, withAlpha(accent, selectAnim * af));
        }

        int flagX = x + 14;
        int flagY = y + 14;
        if (isEn) drawFlagUs(g, flagX, flagY, 42, 22, af);
        else drawFlagRu(g, flagX, flagY, 42, 22, af);

        FontRenderUtility.drawString(g, FontRenderUtility.FontType.SF_BOLD, name, x + 14, y + 50,
            withAlpha(0xFFFFFFFF, af), true);
        FontRenderUtility.drawString(g, FontRenderUtility.FontType.SF_MEDIUM, sub, x + 14, y + 70,
            withAlpha(0xFF9A9EB8, af), false);

        if (selectAnim > 0.01f) {
            int ccx = x + w - 22;
            int ccy = y + 22;
            Render2DUtility.drawRoundQ(g, ccx - 9, ccy - 9, 18, 18, 9, withAlpha(accent, selectAnim * af));
            g.blit(RenderPipelines.GUI_TEXTURED, CHECK, ccx - 7, ccy - 7, 0f, 0f, 14, 14, 256, 256, 256, 256,
                withAlpha(0xFFFFFFFF, selectAnim * af));
        }
    }

    private void drawDone(GuiGraphics g, int centerX, int cardX, int cardY, int cardW, int cardH, int accent, int alpha) {
        float af = alpha / 255f;
        float pop = doneTime < 0f ? 1f : AnimationUtility.Easing.BACK_OUT.apply(clamp01((time - doneTime) / 0.5f));
        int ccy = cardY + 78;
        int rr = Math.round(30 * pop);
        if (rr > 1) {
            Render2DUtility.drawRound(g, centerX - rr, ccy - rr, rr * 2, rr * 2, rr,
                withAlpha(accent, af));
            int checkS = Math.round(rr * 1.33f);
            g.blit(RenderPipelines.GUI_TEXTURED, CHECK, centerX - checkS / 2, ccy - checkS / 2,
                0f, 0f, checkS, checkS, 256, 256, 256, 256, withAlpha(0xFFFFFFFF, af));
        }

        drawCentered(g, FontRenderUtility.FontType.SF_BOLD, LanguageUtility.t("ob_done_title"),
            centerX, cardY + 130, withAlpha(0xFFFFFFFF, af), true);
        drawFitted(g, FontRenderUtility.FontType.SF_MEDIUM, LanguageUtility.t("ob_done_sub"),
            centerX, cardY + 158, cardW - 70, withAlpha(0xFFB8BCD0, af));

        drawPillButton(g, enterRect, LanguageUtility.t("ob_enter"), accent, enterHover, alpha, !transitioning());
    }

    private void drawPillButton(GuiGraphics g, int[] rect, String label, int accent, float hoverAnim, int alpha, boolean interactive) {
        float af = alpha / 255f;
        int x = rect[0];
        int y = rect[1];
        int w = rect[2];
        int h = rect[3];
        int bg = interactive && hoverAnim > 0.02f
            ? ColorUtility.interpolate(accent, brighten(accent, 30), hoverAnim)
            : accent;
        Render2DUtility.drawRoundQ(g, x, y, w, h, 8, withAlpha(bg, af));

        FontRenderUtility.FontType ft = FontRenderUtility.FontType.SF_BOLD;
        int textW = FontRenderUtility.getStringWidth(ft, label);
        int textY = y + (h - FontRenderUtility.getFontHeight()) / 2 + 1;
        FontRenderUtility.drawString(g, ft, label, x + (w - textW) / 2, textY, withAlpha(0xFFFFFFFF, af), true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        if (event.button() != 0) return true;
        if (transitioning()) return true;
        int mx = Math.round((float) ((event.x() - uiX) / uiScale));
        int my = Math.round((float) ((event.y() - uiY) / uiScale));
        if (step == 0) {
            if (hit(contRect, mx, my)) {
                EventBusHolder.get().post(new SoundEvent(SoundEvent.Type.TOGGLE, 0.5f));
                goToStep(1);
            }
        } else if (step == 1) {
            if (hit(enRect, mx, my)) selectLanguage("English");
            else if (hit(ruRect, mx, my)) selectLanguage("Russian");
            else if (hit(contRect, mx, my)) {
                EventBusHolder.get().post(new SoundEvent(SoundEvent.Type.TOGGLE, 0.5f));
                goToStep(2);
            }
        } else if (step == 2) {
            if (hit(enterRect, mx, my)) finish();
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (transitioning()) return true;
            if (step == 0) skip();
            else goToStep(step - 1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_SPACE) {
            advance();
            return true;
        }
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return true;
    }
}
