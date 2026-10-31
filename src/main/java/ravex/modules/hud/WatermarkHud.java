package ravex.modules.hud;

import ravex.modules.annotations.HudModule;
import ravex.modules.annotations.Parameter;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import ravex.RaveX;
import ravex.modules.client.Hud;
import ravex.utility.render.Render2DUtility;
import ravex.utility.render.HudRendererUtility;
import ravex.utility.render.ColorUtility;
import java.io.InputStream;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;

@HudModule("WatermarkHud")
public class WatermarkHud extends ravex.modules.Module {
    @Parameter(name = "Mode", modes = {"Logo+Text", "Logo Only", "Text Only"})
    public String mode = "Logo+Text";
    @Parameter(name = "TextColor", color = true)
    public int textColor = 0xFFFFFFFF;
    @Parameter(name = "Background")
    public boolean background = true;
    @Parameter(name = "CustomText")
    public String customText = "RaveX";
    @Parameter(name = "ShowVersion")
    public boolean showVersion = true;
    // animated reveal when a world is joined
    @Parameter(name = "Intro")
    public boolean intro = false;
    @Parameter(name = "IntroSpeed", min = 0.5, max = 2.0, step = 0.1, visible = "intro")
    public double introSpeed = 1.0;

    private static final Identifier LOGO = Identifier.fromNamespaceAndPath("ravex", "textures/ravexv2");
    private static final int LOGO_W = 1220;
    private static final int LOGO_H = 1396;
    private static boolean logoLoaded = false;

    private static final int VERSION_BLUE = 0xFF2D6CFF;
    private static final int VERSION_WHITE = 0xFFFFFFFF;
    private static final long VERSION_SHIMMER_MS = 2400L;

    private static String cachedVersion;
    private static String[] cachedVersionChars;
    private static int[] cachedVersionWidths;
    private static int cachedVersionWidth = -1;

    private static Object lastLevel;
    private static long introStartMs;
    private static boolean lastIntroEnabled;

    public WatermarkHud() {
        super("WatermarkHud", 10, 10, 110, 22);
        setX(10);
        setY(10);
        setWidth(110);
        setHeight(22);
    }

    private static void ensureLogo() {
        if (logoLoaded) return;
        String path = "/assets/ravex/textures/ravexv2.png";
        try (InputStream stream = WatermarkHud.class.getResourceAsStream(path)) {
            if (stream == null) {
                RaveX.LOGGER.warn("[WatermarkHud] Logo not found: {}", path);
                return;
            }
            NativeImage image = NativeImage.read(stream);
            DynamicTexture tex = new DynamicTexture(() -> "ravexv2", image);
            Render2DUtility.setLinearSampler(tex);
            MinecraftWrapper.getWrapper().getTextureManager().register(LOGO, tex);
            logoLoaded = true;
        } catch (Exception e) {
            RaveX.LOGGER.warn("[WatermarkHud] Failed to load logo: {}", e.getMessage());
        }
    }

    private static float ease(float t) {
        return t < 0f ? 0f : (t > 1f ? 1f : t * t * (3f - 2f * t));
    }

    private static float window(float t, float a, float b) {
        if (t <= a) return 0f;
        if (t >= b) return 1f;
        return ease((t - a) / (b - a));
    }

    // glyphs and widths only change with the version string or the active font
    private static void ensureVersionCache(String ver) {
        int width = HudRendererUtility.textWidth(ver);
        if (ver.equals(cachedVersion) && width == cachedVersionWidth && cachedVersionChars != null) return;

        String[] chars = new String[ver.length()];
        int[] widths = new int[ver.length()];
        for (int i = 0; i < ver.length(); i++) {
            chars[i] = String.valueOf(ver.charAt(i));
            widths[i] = HudRendererUtility.textWidth(chars[i]);
        }
        cachedVersion = ver;
        cachedVersionWidth = width;
        cachedVersionChars = chars;
        cachedVersionWidths = widths;
    }

    // only the text itself shimmers between blue and white, no halo and no shadow copies
    private static void drawVersionShimmer(GuiGraphics graphics, String ver, int x, int y, float reveal) {
        if (ver.isEmpty() || reveal <= 0f) return;
        ensureVersionCache(ver);

        long now = System.currentTimeMillis();
        float cycle = (now % VERSION_SHIMMER_MS) / (float) VERSION_SHIMMER_MS;
        int alpha = (int) (255 * reveal);
        int cx = x;
        for (int i = 0; i < cachedVersionChars.length; i++) {
            float shimmer = 0.5f + 0.5f * (float) Math.sin((cycle + i * 0.12f) * (float) (Math.PI * 2));
            int color = ColorUtility.interpolate(VERSION_BLUE, VERSION_WHITE, shimmer);
            HudRendererUtility.drawText(graphics, cachedVersionChars[i], cx, y,
                ColorUtility.withAlpha(color, alpha), false);
            cx += cachedVersionWidths[i];
        }
    }

    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Modules.enabled(Hud.class)) return;
        if (!logoLoaded) ensureLogo();

        // reveal once per join or toggle, forced done after 8s
        float introT = 1f;
        var mc0 = MinecraftWrapper.getWrapper();
        Object level = mc0.getLevel();
        if (level != lastLevel) {
            lastLevel = level;
            introStartMs = level != null ? System.currentTimeMillis() : 0L;
        }
        if (intro != lastIntroEnabled) {
            lastIntroEnabled = intro;
            if (intro && level != null) introStartMs = System.currentTimeMillis();
        }
        if (intro && logoLoaded && level != null && introStartMs > 0L) {
            long elapsed = System.currentTimeMillis() - introStartMs;
            if (elapsed > 8000L) {
                introT = 1f;
            } else {
                introT = Math.min(1f, elapsed * (float) introSpeed / 2400f);
            }
        }

        boolean showLogo = !"Text Only".equals(mode);
        boolean showText = !"Logo Only".equals(mode);

        float aspect = (float) LOGO_W / LOGO_H;
        int logoH = 16;
        int logoW = (int) (logoH * aspect);

        String text = (customText != null && !customText.isEmpty()) ? customText : "RaveX";

        String verStr = showVersion ? " v" + RaveX.version : "";

        int contentW = 0;
        if (showLogo) contentW += logoW;
        if (showLogo && showText) contentW += 5;
        if (showText) {
            contentW += HudRendererUtility.textWidth(text) + HudRendererUtility.textWidth(verStr);
        }

        int pw = contentW + (background ? 12 : 4);
        int ph = 22;
        setWidth(pw);
        setHeight(ph);

        int bx = getX(), by = getY();

        if (background) {
            float panelA = introT >= 1f ? 1f : ease(Math.min(1f, introT / 0.25f));
            HudRendererUtility.drawBackground(graphics, bx, by, pw, ph, panelA);
        }

        int cx = bx + (background ? 6 : 2);
        if (showLogo) {
            int ly = by + (ph - logoH) / 2;
            float logoScale = 1f;
            if (introT < 1f) {
                logoScale = 0.55f + 0.45f * window(introT, 0f, 0.45f);
            }
            if (logoScale != 1f) {
                graphics.pose().pushMatrix();
                float lcx = cx + logoW / 2f;
                float lcy = ly + logoH / 2f;
                graphics.pose().translate(lcx, lcy);
                graphics.pose().scale(logoScale, logoScale);
                graphics.pose().translate(-lcx, -lcy);
            }
            graphics.blit(LOGO, cx, ly, cx + logoW, ly + logoH, 0.0f, 1.0f, 0.0f, 1.0f);
            if (logoScale != 1f) {
                graphics.pose().popMatrix();
            }
            if (introT < 1f) {
                float sp = (introT - 0.08f) / 0.5f;
                if (sp > 0f && sp < 1f) {
                    float sa = (float) Math.sin(sp * Math.PI);
                    int bandX = cx + (int) (logoW * sp);
                    int bandW = Math.max(2, logoW / 9);
                    Render2DUtility.drawRound(graphics, bandX - bandW, ly, bandW, logoH, 2,
                        ColorUtility.withAlpha(0xFFFFFFFF, (int) (70 * sa)));
                    Render2DUtility.drawRound(graphics, bandX, ly, Math.max(1, bandW / 2), logoH, 2,
                        ColorUtility.withAlpha(0xFFFFFFFF, (int) (140 * sa)));
                }
            }
            cx += logoW + 5;
        }

        if (showText) {
            float textA = introT >= 1f ? 1f : window(introT, 0.3f, 0.65f);
            float shift = introT >= 1f ? 0f : -10f * (1f - textA);
            int tx = cx + (int) shift;
            int ty = by + (ph - HudRendererUtility.fontHeight()) / 2;

            HudRendererUtility.drawText(graphics, text, tx, ty,
                ColorUtility.withAlpha(textColor, (int) (255 * textA)), true);
            tx += HudRendererUtility.textWidth(text);

            if (!verStr.isEmpty()) {
                float verA = introT >= 1f ? 1f : window(introT, 0.6f, 0.9f);
                drawVersionShimmer(graphics, verStr, tx, ty, verA);
            }
        }
    }
}
