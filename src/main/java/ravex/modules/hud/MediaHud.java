package ravex.modules.hud;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import ravex.RaveX;
import ravex.gui.clickgui.ColorUtility;
import ravex.modules.Module;
import ravex.modules.client.Hud;
import ravex.utility.nativelib.NativeLibrary;
import ravex.utility.render.HudRenderer;
import ravex.utility.render.TextureLoader;
import ravex.manager.ModuleManager;
import net.minecraft.client.renderer.texture.AbstractTexture;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MediaHud extends Module {
    private static final Identifier ICON = TextureLoader.HUD_MEDIA_WHITE;
    private static final int IS = HudRenderer.getIconSize();
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_mediaquery");

    private volatile String cachedTitle = "";
    private volatile String cachedArtist = "";
    private volatile boolean cachedPlaying = false;
    private volatile long cachedPosition = 0;
    private volatile long cachedLength = 0;

    private String lastLoadedKey = "";
    private int coverSize = 22;
    private DynamicTexture coverTexture;
    private Identifier coverId;
    private static final int COVER_BORDER = 2;
    private volatile long lastPositionTime;
    private volatile long displayPosition;

    private ScheduledExecutorService scheduler;

    static {
        NATIVE.load();
    }

    private MediaHud() {
        super("Media", 10, 310, 180, 20);
    }

    @Override
    protected void onEnable() {
        startPolling();
    }

    @Override
    protected void onDisable() {
        stopPolling();
        if (coverTexture != null) {
            coverTexture.close();
            coverTexture = null;
        }
        coverId = null;
        cachedTitle = "";
        cachedArtist = "";
        cachedPlaying = false;
        cachedPosition = 0;
        cachedLength = 0;
        lastLoadedKey = "";
    }

    private void startPolling() {
        if (scheduler != null && !scheduler.isShutdown()) return;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "RaveX-MediaQuery");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleWithFixedDelay(this::pollMedia, 0, 2, TimeUnit.SECONDS);
    }

    private void stopPolling() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    private void pollMedia() {
        if (!NATIVE.isLoaded()) return;
        try {
            String raw = nativeGetNowPlaying();
            if (raw == null || raw.isEmpty()) {
                clearCache();
                return;
            }
            String[] parts = raw.split("\\|", 7);
            if (parts.length < 2) {
                clearCache();
                return;
            }
            boolean nowPlaying = "Playing".equals(parts[0]);
            String title = parts[1];
            String artist = parts.length >= 3 ? parts[2] : "";
            String artUrl = parts.length >= 4 ? parts[3] : "";
            String appIcon = parts.length >= 5 ? parts[4] : "";
            long pos = parts.length >= 6 ? parseLongSafe(parts[5]) : 0;
            long len = parts.length >= 7 ? parseLongSafe(parts[6]) : 0;

            cachedTitle = title;
            cachedArtist = artist;
            cachedPlaying = nowPlaying;
            cachedPosition = pos;
            cachedLength = len;
            lastPositionTime = System.nanoTime();

            String key = artUrl.isEmpty() ? appIcon : artUrl;
            if (key.isEmpty() || key.equals(lastLoadedKey)) return;

            RaveX.LOGGER.info("[MediaHud] New track: '{}' | artUrl: {} | icon: {}", title, artUrl, appIcon);

            byte[] imageData = tryDownload(artUrl, appIcon);
            if (imageData != null && imageData.length > 0) {
                RaveX.LOGGER.info("[MediaHud] Cover loaded: {} bytes", imageData.length);
                String capturedKey = key;
                Minecraft.getInstance().execute(() -> {
                    if (registerCover(imageData)) {
                        lastLoadedKey = capturedKey;
                    }
                });
            } else {
                RaveX.LOGGER.warn("[MediaHud] No cover available for: {}", key);
            }
        } catch (Throwable t) {
            RaveX.LOGGER.error("[MediaHud] pollMedia error", t);
            clearCache();
        }
    }

    private byte[] tryDownload(String artUrl, String appIcon) {
        if (!artUrl.isEmpty()) {
            byte[] data = downloadJava(artUrl);
            if (data != null && data.length > 0) {
                RaveX.LOGGER.info("[MediaHud] Java download OK: {} bytes from {}", data.length, artUrl);
                return data;
            }
            RaveX.LOGGER.warn("[MediaHud] Java download failed, trying C for: {}", artUrl);
            data = nativeDownloadArt(artUrl);
            if (data != null && data.length > 0) {
                RaveX.LOGGER.info("[MediaHud] C download OK: {} bytes from {}", data.length, artUrl);
                return data;
            }
        }
        if (!appIcon.isEmpty()) {
            RaveX.LOGGER.info("[MediaHud] Trying app icon: {}", appIcon);
            byte[] data = nativeExtractAppIconForPlayer(appIcon);
            if (data != null && data.length > 0) {
                RaveX.LOGGER.info("[MediaHud] App icon loaded: {} bytes", data.length);
                return data;
            }
        }
        return null;
    }

    private byte[] downloadJava(String url) {
        try {
            if (url.startsWith("file://")) {
                Path path = Paths.get(URI.create(url));
                return Files.readAllBytes(path);
            }
            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestProperty("User-Agent", "RaveX/1.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setInstanceFollowRedirects(true);
            try (InputStream in = conn.getInputStream()) {
                return in.readAllBytes();
            }
        } catch (Throwable t) {
            RaveX.LOGGER.warn("[MediaHud] Java download failed: {} - {}", url, t.getMessage());
            return null;
        }
    }

    private boolean registerCover(byte[] imageData) {
        try {
            NativeImage original = NativeImage.read(new ByteArrayInputStream(imageData));
            
            // Resize to 128x128 using high-quality STB image resize for perfect anti-aliased sharpness
            NativeImage resized = new NativeImage(original.format(), 128, 128, false);
            original.resizeSubRectTo(0, 0, original.getWidth(), original.getHeight(), resized);
            original.close();

            Identifier texId = Identifier.fromNamespaceAndPath("ravex", "media_cover");
            if (coverTexture != null) coverTexture.close();
            coverTexture = new DynamicTexture(() -> "ravex:media_cover", resized);
            coverId = texId;
            Minecraft.getInstance().getTextureManager().register(texId, coverTexture);
            
            // Set bilinear filter (LINEAR) for smooth album art scaling and no pixelation
            try {
                com.mojang.blaze3d.textures.GpuSampler linearSampler = com.mojang.blaze3d.systems.RenderSystem.getSamplerCache()
                    .getClampToEdge(com.mojang.blaze3d.textures.FilterMode.LINEAR);
                for (Field f : AbstractTexture.class.getDeclaredFields()) {
                    if (com.mojang.blaze3d.textures.GpuSampler.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        f.set(coverTexture, linearSampler);
                        break;
                    }
                }
            } catch (Throwable t) {
                RaveX.LOGGER.warn("[MediaHud] Failed to set bilinear filter: {}", t.getMessage());
            }
            
            return true;
        } catch (Throwable t) {
            RaveX.LOGGER.warn("[MediaHud] registerCover failed: {}", t.getMessage());
            return false;
        }
    }

    private void clearCache() {
        cachedTitle = "";
        cachedArtist = "";
        cachedPlaying = false;
        cachedPosition = 0;
        cachedLength = 0;
    }

    private static long parseLongSafe(String s) {
        try { return Long.parseLong(s); } catch (Throwable t) { return 0; }
    }

    private static String formatTime(long micros) {
        long secs = micros / 1000000;
        long min = secs / 60;
        long sec = secs % 60;
        return String.format("%d:%02d", min, sec);
    }

    private static void drawScrollingText(GuiGraphics graphics, String text, int x, int y, int maxWidth, int color, boolean shadow) {
        int textW = HudRenderer.textWidth(text);
        if (textW <= maxWidth) {
            HudRenderer.drawText(graphics, text, x, y, color, shadow);
            return;
        }

        String extended = text + "      ";
        int extW = HudRenderer.textWidth(extended);

        double speed = 20.0; // pixels per second
        double timeSecs = System.currentTimeMillis() / 1000.0;
        int scrollX = (int) ((timeSecs * speed) % extW);

        // Clip the marquee to the text area
        ravex.utility.render.Render2DEngine.pushScissor(graphics, x, y - 1, maxWidth, HudRenderer.fontHeight() + 3);
        
        // Draw first iteration
        HudRenderer.drawText(graphics, extended, x - scrollX, y, color, shadow);
        // Draw second iteration to make the loop seamless
        if (x - scrollX + extW < x + maxWidth) {
            HudRenderer.drawText(graphics, extended, x - scrollX + extW, y, color, shadow);
        }
        
        ravex.utility.render.Render2DEngine.popScissor(graphics);
    }

    @Override
    public void render(GuiGraphics graphics, float partialTicks) {
        if (!ModuleManager.get(Hud.class).getEnabled()) return;

        String title = cachedTitle;
        if (title.isEmpty()) return;

        int activeColor = ColorUtility.getActiveColor();
        int bx = getX();
        int by = getY();
        int pw = 160;
        int ph = 42;
        
        setWidth(pw);
        setHeight(ph);

        long pos = cachedPosition;
        if (cachedPlaying && cachedLength > 0) {
            long elapsed = (System.nanoTime() - lastPositionTime) / 1000;
            pos = Math.min(pos + elapsed, cachedLength);
        }
        displayPosition = pos;

        boolean hasArt = coverTexture != null && coverId != null;

        // Draw card background with active-color matching border (glassmorphism style)
        int bgColor = 0x800C0C0C;
        int borderColor = ravex.gui.clickgui.ColorUtility.withAlpha(activeColor, 75);
        ravex.utility.render.Render2DEngine.drawRoundedRectWithBorder(graphics, bx, by, pw, ph, 5, bgColor, borderColor, 1);

        if (hasArt) {
            // Draw album art (now 32x32 size)
            graphics.blit(coverId, bx + 5, by + 5, bx + 37, by + 37, 0.0F, 1.0F, 0.0F, 1.0F);
            // Draw a subtle 1px border around the cover art
            ravex.utility.render.Render2DEngine.drawRoundBorder(graphics, bx + 5, by + 5, 32, 32, 1, 1, 0x30FFFFFF);
        } else {
            // Draw music note placeholder
            int placeholderBg = 0x15FFFFFF;
            ravex.utility.render.Render2DEngine.drawRound(graphics, bx + 5, by + 5, 32, 32, 4, placeholderBg);
            HudRenderer.drawIcon(graphics, ICON, bx + 5 + (32 - IS) / 2, by + 5 + (32 - IS) / 2, activeColor);
        }

        int textX = bx + 42;
        int maxTextWidth = 160 - 42 - 5; // 113 pixels

        String titleStr = (cachedPlaying ? "\u25B6 " : "\u23F8 ") + title;
        String artistStr = cachedPlaying && !cachedArtist.isEmpty() ? cachedArtist : "";
        String timeStr = "";
        if (cachedLength > 0) {
            timeStr = formatTime(pos) + " / " + formatTime(cachedLength);
        } else {
            timeStr = "0:00 / 0:00";
        }

        // Draw butter-smooth scrolling texts
        drawScrollingText(graphics, titleStr, textX, by + 4, maxTextWidth, cachedPlaying ? 0xFFE0E0FF : 0xFF8080A0, true);
        
        if (!artistStr.isEmpty()) {
            drawScrollingText(graphics, artistStr, textX, by + 13, maxTextWidth, 0xFF80809A, false);
            HudRenderer.drawText(graphics, timeStr, textX, by + 22, 0xFF606080, false);
        } else {
            HudRenderer.drawText(graphics, timeStr, textX, by + 16, 0xFF606080, false);
        }

        // Draw sleek progress bar at the bottom edge
        if (cachedLength > 0) {
            float progress = Math.min(1f, (float) pos / (float) cachedLength);
            int barW = pw - 2; // match border rounding
            int filled = (int) (barW * progress);
            
            // Progress background
            graphics.fill(bx + 1, by + ph - 2, bx + pw - 1, by + ph - 1, 0x15FFFFFF);
            // Progress fill
            if (filled > 0) {
                graphics.fill(bx + 1, by + ph - 2, bx + 1 + filled, by + ph - 1, activeColor);
            }
        }
    }

    private static native String nativeGetNowPlaying();
    private static native boolean nativeIsAvailable();
    private static native byte[] nativeDownloadArt(String url);
    private static native byte[] nativeExtractAppIcon();
    private static native byte[] nativeExtractAppIconForPlayer(String playerName);

    public static boolean maybeEnabled() {
        return maybeEnabled(MediaHud.class);
    }

    public static MediaHud itz() {
        return ModuleManager.get(MediaHud.class);
    }
}
