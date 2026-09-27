package ravex.utility.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
import ravex.modules.client.MainMenu;
import ravex.modules.client.Settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class MenuBackgroundUtility {
    private static int TEX_W = 960;
    private static int TEX_H = 540;
    private static int PIX_COUNT = TEX_W * TEX_H;
    private static final int APPLY_CHUNK = 180;
    private static final long UPDATE_MS = 50L;
    private static final long GIF_ROTATE_MS = 30_000L;
    private static final String[] GIF_IDS = {"gif1", "gif2", "gif3", "gif4", "gif5", "gif6", "gif7", "gif8", "gif9", "gif10"};
    private static final String[] EMPTY_GIFS = new String[0];
    private static final Identifier TEXTURE_ID = Identifier.fromNamespaceAndPath("ravex", "menu_bg");

    private static volatile String[] customGifs = EMPTY_GIFS;
    private static volatile boolean customSource;
    private static volatile long customScanAtMs;

    private static final int[] BAYER_4 = {
        0, 8, 2, 10,
        12, 4, 14, 6,
        3, 11, 1, 9,
        15, 7, 13, 5
    };

    private static NativeImage image;
    private static DynamicTexture texture;

    private static volatile int latestColor = 0xFF0066FF;
    private static volatile int[] pendingFrame;
    private static volatile boolean workerStarted;
    private static volatile boolean applyBusy;

    private static int[] applyingFrame;
    private static int applyY;

    private static volatile GifUtility.Clip gifClip;
    private static volatile String gifLoadedId = "";
    private static volatile String gifWantedId = "";
    private static volatile boolean gifLoading;
    private static int gifFrameIndex;
    private static long gifNextAtMs;
    private static long gifRotateAtMs;
    private static boolean gifPrimed;
    private static boolean gifCleared;

    private static double[] colSunDx2;
    private static double[] colWarm;
    private static double[] colInvWarmDist;
    private static double[] colLitSide;
    private static double[] colReflU;
    private static double[] colRidge1;
    private static double[] colRidge2;
    private static double[] colRidge3;
    private static double[] colCloudU;
    private static double[] colCloudU2;
    private static double[] colBandU;
    private static double[] colTexU;
    private static double[] colTexU2;

    private MenuBackgroundUtility() {}

    private static void allocColumns() {
        colSunDx2 = new double[TEX_W];
        colWarm = new double[TEX_W];
        colInvWarmDist = new double[TEX_W];
        colLitSide = new double[TEX_W];
        colReflU = new double[TEX_W];
        colRidge1 = new double[TEX_W];
        colRidge2 = new double[TEX_W];
        colRidge3 = new double[TEX_W];
        colCloudU = new double[TEX_W];
        colCloudU2 = new double[TEX_W];
        colBandU = new double[TEX_W];
        colTexU = new double[TEX_W];
        colTexU2 = new double[TEX_W];
    }

    private static void ensureTexture(boolean gifMode) {
        if (texture != null) return;
        var window = MinecraftWrapper.getWrapper().getWindow();
        if (window != null && window.getWidth() > 0 && window.getHeight() > 0) {
            TEX_W = Math.max(640, window.getWidth());
            TEX_H = Math.max(360, window.getHeight());
        }
        PIX_COUNT = TEX_W * TEX_H;
        allocColumns();
        image = new NativeImage(TEX_W, TEX_H, false);
        texture = new DynamicTexture(() -> "menu_bg", image);
        Render2DUtility.setLinearSampler(texture);
        MinecraftWrapper.getWrapper().getTextureManager().register(TEXTURE_ID, texture);
        int[] first = new int[PIX_COUNT];
        if (gifMode) {
            Arrays.fill(first, 0xFF000000);
            gifCleared = true;
        } else {
            renderPixels(first, 0.0, latestColor);
        }
        applyRows(first, 0, TEX_H);
        texture.upload();
        ensureWorker();
    }

    private static boolean queueBlackFrame() {
        if (pendingFrame != null || applyBusy) return false;
        int[] black = new int[PIX_COUNT];
        Arrays.fill(black, 0xFF000000);
        pendingFrame = black;
        return true;
    }

    public static void render(GuiGraphics graphics) {
        latestColor = resolveColor();
        boolean gifMode = resolveGifMode();

        if (!gifMode) {
            gifClip = null;
            gifLoadedId = "";
            gifWantedId = "";
            gifPrimed = false;
            gifRotateAtMs = 0L;
            gifCleared = false;
        } else {
            long now = System.currentTimeMillis();
            if (gifWantedId.isEmpty() || now >= gifRotateAtMs) {
                gifRotateAtMs = now + GIF_ROTATE_MS;
                if (customSource) refreshCustomGifs(true);
                ensureGifLoaded(pickDifferentGif(gifPool(), gifWantedId.isEmpty() ? gifLoadedId : gifWantedId));
            } else {
                ensureGifLoaded(gifWantedId);
            }
            if (!gifPrimed && !gifCleared) {
                if (texture == null || queueBlackFrame()) {
                    gifCleared = true;
                }
            }
        }

        if (texture == null) {
            ensureTexture(gifMode);
        } else {
            tickApply();
            ensureWorker();
        }

        int gw = graphics.guiWidth();
        int gh = graphics.guiHeight();
        float scale = Math.max(gw / (float) TEX_W, gh / (float) TEX_H);
        int dw = Math.round(TEX_W * scale);
        int dh = Math.round(TEX_H * scale);
        int dx = (gw - dw) / 2;
        int dy = (gh - dh) / 2;
        Render2DUtility.pushScissor(graphics, 0, 0, gw, gh);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE_ID,
            dx, dy, 0f, 0f,
            dw, dh,
            TEX_W, TEX_H, TEX_W, TEX_H);
        Render2DUtility.popScissor(graphics);
    }

    private static boolean resolveGifMode() {
        MainMenu menu = Modules.get(MainMenu.class);
        if (menu == null) {
            customSource = false;
            return false;
        }
        if ("Custom".equals(menu.mode)) {
            customSource = true;
            refreshCustomGifs(false);
            return customGifs.length > 0;
        }
        customSource = false;
        return "GIF".equals(menu.bgMode);
    }

    private static void refreshCustomGifs(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && (customGifs.length > 0 || now < customScanAtMs)) return;
        customScanAtMs = now + 2000L;
        File folder = MainMenu.getGifsFolder();
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".gif"));
        if (files == null || files.length == 0) {
            customGifs = EMPTY_GIFS;
            return;
        }
        Arrays.sort(files);
        String[] paths = new String[files.length];
        for (int i = 0; i < files.length; i++) paths[i] = files[i].getAbsolutePath();
        customGifs = paths;
    }

    private static String[] gifPool() {
        String[] pool = customGifs;
        if (customSource && pool.length > 0) return pool;
        return GIF_IDS;
    }

    private static String pickDifferentGif(String[] pool, String current) {
        if (pool.length == 0) return "";
        if (pool.length == 1) return pool[0];
        String pick;
        do {
            pick = pool[ThreadLocalRandom.current().nextInt(pool.length)];
        } while (pick.equals(current));
        return pick;
    }

    private static void ensureGifLoaded(String id) {
        if (id == null || id.isEmpty()) return;
        gifWantedId = id;
        if (gifLoading) return;
        if (gifClip != null && id.equals(gifLoadedId)) return;
        gifLoading = true;
        String loadId = id;
        Thread loader = new Thread(() -> {
            try {
                GifUtility.Clip clip;
                if (loadId.indexOf('/') >= 0 || loadId.indexOf('\\') >= 0) {
                    try (InputStream stream = new FileInputStream(loadId)) {
                        clip = GifUtility.load(stream);
                    }
                } else {
                    String path = "/assets/ravex/img/gif/" + loadId + ".gif";
                    try (InputStream stream = MenuBackgroundUtility.class.getResourceAsStream(path)) {
                        if (stream == null) return;
                        clip = GifUtility.load(stream);
                    }
                }
                if (loadId.equals(gifWantedId)) {
                    gifClip = clip;
                    gifLoadedId = loadId;
                    gifFrameIndex = 0;
                    gifNextAtMs = 0L;
                    gifPrimed = false;
                }
            } catch (Throwable ignored) {
            } finally {
                gifLoading = false;
            }
        }, "ravex-gif-load");
        loader.setDaemon(true);
        loader.setPriority(Thread.MIN_PRIORITY);
        loader.start();
    }

    private static void queueGifFrame(long now) {
        GifUtility.Clip clip = gifClip;
        if (clip == null || clip.frames.length == 0) return;
        if (gifFrameIndex >= clip.frames.length) gifFrameIndex = 0;
        int[] frame = new int[PIX_COUNT];
        packCover(clip.frames[gifFrameIndex], clip.width, clip.height, frame);
        pendingFrame = frame;
        int delay = clip.delaysMs[Math.min(gifFrameIndex, clip.delaysMs.length - 1)];
        gifNextAtMs = now + delay;
        gifFrameIndex++;
        if (gifFrameIndex >= clip.frames.length) gifFrameIndex = 0;
        gifPrimed = true;
    }

    private static void packCover(int[] src, int sw, int sh, int[] dst) {
        if (sw <= 0 || sh <= 0) {
            Arrays.fill(dst, 0xFF000000);
            return;
        }
        float scale = Math.max(TEX_W / (float) sw, TEX_H / (float) sh);
        int scaledW = Math.max(1, Math.round(sw * scale));
        int scaledH = Math.max(1, Math.round(sh * scale));
        int offX = (TEX_W - scaledW) / 2;
        int offY = (TEX_H - scaledH) / 2;
        Arrays.fill(dst, 0xFF000000);
        float invScale = 1.0f / scale;
        for (int y = 0; y < scaledH; y++) {
            int dy = y + offY;
            if (dy < 0 || dy >= TEX_H) continue;
            float syf = (y + 0.5f) * invScale - 0.5f;
            if (syf < 0f) syf = 0f;
            float maxY = sh - 1;
            if (syf > maxY) syf = maxY;
            int sy0 = (int) syf;
            int sy1 = sy0 < sh - 1 ? sy0 + 1 : sy0;
            float fy = syf - sy0;
            int row0 = sy0 * sw;
            int row1 = sy1 * sw;
            int drow = dy * TEX_W;
            for (int x = 0; x < scaledW; x++) {
                int dx = x + offX;
                if (dx < 0 || dx >= TEX_W) continue;
                float sxf = (x + 0.5f) * invScale - 0.5f;
                if (sxf < 0f) sxf = 0f;
                float maxX = sw - 1;
                if (sxf > maxX) sxf = maxX;
                int sx0 = (int) sxf;
                int sx1 = sx0 < sw - 1 ? sx0 + 1 : sx0;
                float fx = sxf - sx0;
                int c00 = src[row0 + sx0];
                int c10 = src[row0 + sx1];
                int c01 = src[row1 + sx0];
                int c11 = src[row1 + sx1];
                float w00 = (1f - fx) * (1f - fy);
                float w10 = fx * (1f - fy);
                float w01 = (1f - fx) * fy;
                float w11 = fx * fy;
                int r = (int) (((c00 >> 16) & 0xFF) * w00 + ((c10 >> 16) & 0xFF) * w10
                    + ((c01 >> 16) & 0xFF) * w01 + ((c11 >> 16) & 0xFF) * w11);
                int g = (int) (((c00 >> 8) & 0xFF) * w00 + ((c10 >> 8) & 0xFF) * w10
                    + ((c01 >> 8) & 0xFF) * w01 + ((c11 >> 8) & 0xFF) * w11);
                int b = (int) ((c00 & 0xFF) * w00 + (c10 & 0xFF) * w10
                    + (c01 & 0xFF) * w01 + (c11 & 0xFF) * w11);
                if (r > 255) r = 255;
                if (g > 255) g = 255;
                if (b > 255) b = 255;
                dst[drow + dx] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        }
    }

    private static void tickApply() {
        if (applyingFrame == null) {
            int[] pending = pendingFrame;
            if (pending != null) {
                pendingFrame = null;
                applyingFrame = pending;
                applyY = 0;
                applyBusy = true;
            }
        }

        if (applyingFrame == null) return;

        int end = applyY + APPLY_CHUNK;
        if (end > TEX_H) end = TEX_H;
        applyRows(applyingFrame, applyY, end);
        applyY = end;

        if (applyY >= TEX_H) {
            texture.upload();
            applyingFrame = null;
            applyY = 0;
            applyBusy = false;
        }
    }

    private static void applyRows(int[] frame, int y0, int y1) {
        for (int y = y0; y < y1; y++) {
            int row = y * TEX_W;
            for (int x = 0; x < TEX_W; x++) {
                image.setPixel(x, y, frame[row + x]);
            }
        }
    }

    private static int resolveColor() {
        Settings settings = Modules.get(Settings.class);
        if (settings == null) return 0xFF0066FF;
        return settings.menuColor;
    }

    private static void ensureWorker() {
        if (workerStarted) return;
        synchronized (MenuBackgroundUtility.class) {
            if (workerStarted) return;
            Thread worker = new Thread(MenuBackgroundUtility::workerLoop, "ravex-menu-bg");
            worker.setDaemon(true);
            worker.setPriority(Thread.MIN_PRIORITY);
            worker.start();
            workerStarted = true;
        }
    }

    private static void workerLoop() {
        double time = 0.0;
        long last = System.currentTimeMillis();
        int lastColor = latestColor;

        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(10L);
            } catch (InterruptedException e) {
                return;
            }

            boolean gifMode = resolveGifMode();
            int color = latestColor;
            long now = System.currentTimeMillis();

            if (gifMode) {
                if (pendingFrame != null) continue;
                GifUtility.Clip clip = gifClip;
                if (clip == null) continue;
                if (!gifPrimed || now >= gifNextAtMs) {
                    queueGifFrame(now);
                }
                last = now;
                lastColor = color;
                continue;
            }

            long elapsed = now - last;
            if (elapsed < UPDATE_MS && color == lastColor) continue;
            if (elapsed > 250L) elapsed = 250L;
            if (pendingFrame != null) continue;

            last = now;
            lastColor = color;
            time += elapsed / 1000.0;

            int[] frame = new int[PIX_COUNT];
            renderPixels(frame, time, color);
            pendingFrame = frame;
        }
    }

    private static void renderPixels(int[] frame, double t, int color) {
        float tintR = ((color >> 16) & 0xFF) / 255.0f;
        float tintG = ((color >> 8) & 0xFF) / 255.0f;
        float tintB = (color & 0xFF) / 255.0f;
        float mulR = 0.70f + 0.40f * tintR;
        float mulG = 0.70f + 0.40f * tintG;
        float mulB = 0.70f + 0.40f * tintB;

        double sunX = 0.5 + 0.16 * Math.sin(t * 0.11);
        double sunY = 0.44 + 0.025 * Math.sin(t * 0.07);
        double horizon = 0.575;
        double invW = 1.0 / (TEX_W - 1);
        double invH = 1.0 / (TEX_H - 1);

        for (int x = 0; x < TEX_W; x++) {
            double u = x * invW;
            double sunDx = u - sunX;
            double absDx = sunDx < 0 ? -sunDx : sunDx;
            colSunDx2[x] = sunDx * sunDx;
            colWarm[x] = Math.exp(-absDx * 3.0);
            colInvWarmDist[x] = Math.exp(-absDx * 2.2);
            colLitSide[x] = Math.exp(-absDx * 2.5);
            colReflU[x] = Math.exp(-absDx * 8.0);
            colRidge1[x] = horizon + 0.012
                + 0.028 * Math.sin(u * 6.1 + 1.7)
                + 0.016 * Math.sin(u * 13.0 + 0.4)
                + 0.008 * Math.sin(u * 27.0 + 2.8);
            colRidge2[x] = horizon + 0.055
                + 0.040 * Math.sin(u * 4.7 + 4.2)
                + 0.022 * Math.sin(u * 10.0 + 2.0)
                + 0.012 * Math.sin(u * 21.0 + 3.7);
            colRidge3[x] = horizon + 0.125
                + 0.055 * Math.sin(u * 3.3 + 0.8)
                + 0.030 * Math.sin(u * 8.0 + 5.0)
                + 0.016 * Math.sin(u * 17.0 + 2.2);
            colCloudU[x] = u * 4.0 + t * 0.055;
            colCloudU2[x] = u * 11.0 - t * 0.03;
            colBandU[x] = u * 16.0 + t * 0.3;
            colTexU[x] = u * 34.0;
            colTexU2[x] = u * 58.0 + 7.0;
        }

        for (int y = 0; y < TEX_H; y++) {
            double v = y * invH;
            double sunDy = v - sunY;
            double rowSunDy2 = sunDy * sunDy * 0.72;
            double absHoriz = v - horizon;
            if (absHoriz < 0) absHoriz = -absHoriz;
            double horizonGlow = Math.exp(-absHoriz * 7.0);
            double bandMaskBase = Math.exp(-absHoriz * 16.0);
            boolean isSky = v < horizon;
            double skyT = isSky ? v / horizon : 0.0;
            double groundT = isSky ? 0.0 : (v - horizon) / (1.0 - horizon);
            double rowCloudV = v * 9.0 + t * 0.012;
            double rowCloudV2 = v * 20.0 + 5.0;
            double rowTexV = v * 66.0;
            double rowTexV2 = v * 96.0 + 13.0;
            double rowReflV = isSky ? 0.0 : Math.exp(-(v - horizon) * 7.5);
            int rowBase = y * TEX_W;
            int ditherRow = (y & 3) << 2;
            boolean cloudZone = isSky && skyT > 0.04 && skyT < 0.96;
            double invSkyWarm = 1.0 - skyT * 0.5;

            for (int x = 0; x < TEX_W; x++) {
                double sunDist = Math.sqrt(colSunDx2[x] + rowSunDy2);
                double r, g, b;

                if (isSky) {
                    double sunGlow = Math.exp(-sunDist * 7.0);
                    double sunHalo = sunDist < 0.55 ? Math.exp(-sunDist * 2.6) : 0.0;
                    double rays = 0.0;
                    if (sunDist < 0.38) {
                        double rayAngle = Math.atan2(sunDy, Math.sqrt(colSunDx2[x]) + 1.0e-6);
                        double s1 = Math.abs(Math.sin(rayAngle * 6.0 + t * 0.2));
                        double s2 = Math.abs(Math.sin(rayAngle * 9.0 - t * 0.14 + 1.2));
                        double decay = Math.exp(-sunDist * 5.2);
                        rays = (pow24(s1) * 0.4 + pow32(s2) * 0.2) * decay;
                    }
                    double sunCore = sunDist < 0.045 ? smoothstep(0.03, 0.012, sunDist) : 0.0;
                    double sunInner = sunDist < 0.09 ? Math.exp(-sunDist * 20.0) : 0.0;

                    double sr = 0.035 + 0.065 * skyT;
                    double sg = 0.025 + 0.125 * skyT;
                    double sb = 0.14 + 0.28 * skyT;

                    double warm = colWarm[x] * invSkyWarm;
                    sr += (0.92 - sr) * warm * 0.78;
                    sg += (0.42 - sg) * warm * 0.58;
                    sb += (0.15 - sb) * warm * 0.40;

                    double bandWave = 0.5 + 0.5 * Math.sin(colBandU[x] + skyT * 11.0);
                    double bandMask = bandMaskBase * bandWave;
                    sr += (1.0 - sr) * bandMask * 0.42;
                    sg += (0.55 - sg) * bandMask * 0.34;
                    sb += (0.24 - sb) * bandMask * 0.24;

                    double hglow = horizonGlow * (0.5 + 0.5 * colInvWarmDist[x]);
                    sr += (1.0 - sr) * hglow * 0.58;
                    sg += (0.58 - sg) * hglow * 0.50;
                    sb += (0.30 - sb) * hglow * 0.36;

                    if (cloudZone) {
                        double cloudN = fbm3(colCloudU[x], rowCloudV) * 0.75
                            + valueNoise(colCloudU2[x], rowCloudV2) * 0.25;
                        double cloudBand = smoothstep(0.47, 0.62, cloudN)
                            * smoothstep(0.04, 0.28, skyT)
                            * (1.0 - smoothstep(0.72, 0.96, skyT));

                        if (cloudBand > 0.01) {
                            double lit = colLitSide[x];
                            double cr = 0.14 + 0.84 * lit;
                            double cg = 0.09 + 0.43 * lit;
                            double cb = 0.20 + 0.02 * lit;
                            sr += (cr - sr) * cloudBand * 0.82;
                            sg += (cg - sg) * cloudBand * 0.76;
                            sb += (cb - sb) * cloudBand * 0.58;

                            double edge = smoothstep(0.58, 0.72, cloudN) - smoothstep(0.47, 0.55, cloudN);
                            double rim = edge * lit * cloudBand;
                            sr += rim * 0.45;
                            sg += rim * 0.22;
                            sb += rim * 0.08;
                        }
                    }

                    r = sr + sunGlow * 1.05 + sunHalo * 0.5 + rays + sunInner * 1.5 + sunCore * 1.2;
                    g = sg + sunGlow * 0.58 + sunHalo * 0.28 + rays * 0.6 + sunInner * 0.95 + sunCore * 1.05;
                    b = sb + sunGlow * 0.16 + sunHalo * 0.09 + rays * 0.16 + sunInner * 0.34 + sunCore * 0.7;

                    if (skyT < 0.38 && ((x * 17 + y * 31) & 63) < 2 && hash01(x, y) > 0.7) {
                        double twinkle = 0.55 + 0.45 * Math.sin(t * 3.0 + x * 0.7 + y * 0.3);
                        double fade = 1.0 - smoothstep(0.1, 0.38, skyT);
                        double s = twinkle * fade;
                        r += s * 0.85;
                        g += s * 0.8;
                        b += s * 0.75;
                    }
                } else {
                    double gr = 0.14 + (0.015 - 0.14) * groundT;
                    double gg = 0.07 + (0.015 - 0.07) * groundT;
                    double gb = 0.11 + (0.05 - 0.11) * groundT;

                    double ridge1 = colRidge1[x];
                    double ridge2 = colRidge2[x];
                    double ridge3 = colRidge3[x];
                    double aa = 0.0016;
                    double m1 = smoothstep(ridge1 + aa, ridge1 - aa, v);
                    double m2 = smoothstep(ridge2 + aa, ridge2 - aa, v);
                    double m3 = smoothstep(ridge3 + aa, ridge3 - aa, v);

                    if (m1 > 0.0) {
                        double tex = valueNoise(colTexU[x], rowTexV) * 0.1;
                        gr += (0.10 + tex - gr) * m1;
                        gg += (0.055 + tex * 0.6 - gg) * m1;
                        gb += (0.11 + tex * 0.4 - gb) * m1;

                        double dR = v - ridge1;
                        if (dR > -0.007 && dR < 0.007) {
                            double ad = dR < 0 ? -dR : dR;
                            double rim = Math.exp(-ad * 260.0) * m1;
                            double rw = 0.5 + 0.5 * colWarm[x];
                            gr += rim * rw * 1.0;
                            gg += rim * rw * 0.48;
                            gb += rim * rw * 0.15;
                        }
                    }

                    if (m2 > 0.0) {
                        double tex = valueNoise(colTexU2[x], rowTexV2) * 0.06;
                        gr += (0.05 + tex - gr) * m2;
                        gg += (0.03 + tex * 0.6 - gg) * m2;
                        gb += (0.075 + tex * 0.4 - gb) * m2;

                        double dR = v - ridge2;
                        if (dR > -0.005 && dR < 0.005) {
                            double ad = dR < 0 ? -dR : dR;
                            double rim = Math.exp(-ad * 300.0) * m2;
                            double rw = 0.5 + 0.5 * colWarm[x];
                            gr += rim * rw * 0.4;
                            gg += rim * rw * 0.18;
                            gb += rim * rw * 0.06;
                        }
                    }

                    if (m3 > 0.0) {
                        double tex = valueNoise(colTexU2[x] + 13.0, rowTexV) * 0.04;
                        gr += (0.02 + tex - gr) * m3;
                        gg += (0.018 + tex * 0.5 - gg) * m3;
                        gb += (0.045 + tex * 0.3 - gb) * m3;
                    }

                    double refl = colReflU[x] * rowReflV;
                    if (refl > 0.02) {
                        refl *= 0.55 + 0.45 * Math.sin(v * 110.0 + t * 1.4);
                        refl *= 0.5 + 0.5 * valueNoise(colTexU[x], v * 130.0 + t * 0.5);
                        gr += refl * 0.75;
                        gg += refl * 0.36;
                        gb += refl * 0.1;
                    }

                    double sunHalo = sunDist < 0.5 ? Math.exp(-sunDist * 2.8) : 0.0;
                    r = gr + sunHalo * 0.1;
                    g = gg + sunHalo * 0.05;
                    b = gb + sunHalo * 0.025;
                }

                r *= mulR;
                g *= mulG;
                b *= mulB;

                double dx = (x * invW) - 0.5;
                double dy = v - 0.5;
                double vig = 1.0 - (dx * dx + dy * dy * 0.8) * 1.2;
                if (vig < 0.0) vig = 0.0;
                vig = vig * vig * (3.0 - 2.0 * vig);
                r *= vig;
                g *= vig;
                b *= vig;

                if (r > 1.0) r = 1.0;
                if (g > 1.0) g = 1.0;
                if (b > 1.0) b = 1.0;

                int dither = BAYER_4[ditherRow + (x & 3)] - 8;
                int ri = (int) (r * 255.0 + dither * 0.4);
                int gi = (int) (g * 255.0 + dither * 0.4);
                int bi = (int) (b * 255.0 + dither * 0.4);
                if (ri < 0) ri = 0;
                else if (ri > 255) ri = 255;
                if (gi < 0) gi = 0;
                else if (gi > 255) gi = 255;
                if (bi < 0) bi = 0;
                else if (bi > 255) bi = 255;
                frame[rowBase + x] = 0xFF000000 | (ri << 16) | (gi << 8) | bi;
            }
        }
    }

    private static double pow24(double x) {
        double x2 = x * x;
        double x4 = x2 * x2;
        double x8 = x4 * x4;
        double x16 = x8 * x8;
        return x16 * x4 * x2;
    }

    private static double pow32(double x) {
        double x2 = x * x;
        double x4 = x2 * x2;
        double x8 = x4 * x4;
        double x16 = x8 * x8;
        return x16 * x16;
    }

    private static double fbm3(double x, double y) {
        double v = valueNoise(x, y) * 0.5714;
        v += valueNoise(x * 2.1, y * 2.1) * 0.2857;
        v += valueNoise(x * 4.41, y * 4.41) * 0.1429;
        return v;
    }

    private static double valueNoise(double x, double y) {
        int xi = (int) Math.floor(x);
        int yi = (int) Math.floor(y);
        double xf = x - xi;
        double yf = y - yi;
        double u = xf * xf * (3.0 - 2.0 * xf);
        double v = yf * yf * (3.0 - 2.0 * yf);
        double a = hash01(xi, yi);
        double b = hash01(xi + 1, yi);
        double c = hash01(xi, yi + 1);
        double d = hash01(xi + 1, yi + 1);
        double ab = a + (b - a) * u;
        double cd = c + (d - c) * u;
        return ab + (cd - ab) * v;
    }

    private static double hash01(int x, int y) {
        long n = (x * 0x9E3779B1L) ^ (y * 0x85EBCA77L);
        n = (n ^ (n >>> 16)) * 0x85EBCA6BL;
        n ^= n >>> 15;
        return (n & 0xFFFF) * (1.0 / 65535.0);
    }

    private static double smoothstep(double edge0, double edge1, double x) {
        double t = (x - edge0) / (edge1 - edge0);
        if (t < 0.0) t = 0.0;
        else if (t > 1.0) t = 1.0;
        return t * t * (3.0 - 2.0 * t);
    }
}
