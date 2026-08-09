package ravex.modules.hud;
import ravex.modules.annotations.Module;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import ravex.RaveX;
import ravex.utility.render.ColorUtility;

import ravex.modules.client.Hud;
import ravex.utility.render.HudRendererUtility;
import ravex.utility.render.TextureLoaderUtility;

import net.minecraft.client.renderer.texture.AbstractTexture;
import ravex.utility.system.SystemUtility;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
import org.jetbrains.annotations.Nullable;

@Module(name = "MediaHud", category = "HUD")
public class MediaHud extends ravex.modules.Module {
    public int x;
    public int y;
    public int width;
    public int height;
private static final Identifier ICON = TextureLoaderUtility.HUD_MEDIA_WHITE;
    private static final int IS = HudRendererUtility.getIconSize();

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

    private MediaHud() {
        super("MediaHud", 2, 100, 200, 60);
        this.x = 10; this.y = 310; this.width = 180; this.height = 20;
    }
    protected void onEnable() {
        startPolling();
    }
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
        try {
            String raw = SystemUtility.getNowPlaying();
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
                MinecraftWrapper.getWrapper().execute(() -> {
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

    @Nullable
    private byte[] tryDownload(String artUrl, String appIcon) {
        if (!artUrl.isEmpty()) {
            byte[] data = SystemUtility.downloadArt(artUrl);
            if (data != null && data.length > 0) {
                RaveX.LOGGER.info("[MediaHud] Download OK: {} bytes from {}", data.length, artUrl);
                return data;
            }
        }
        if (!appIcon.isEmpty()) {
            RaveX.LOGGER.info("[MediaHud] Trying app icon: {}", appIcon);
            byte[] data = SystemUtility.getAppIcon(appIcon);
            if (data != null && data.length > 0) {
                RaveX.LOGGER.info("[MediaHud] App icon loaded: {} bytes", data.length);
                return data;
            }
        }
        return null;
    }

    private boolean registerCover(byte[] imageData) {
        try {
            NativeImage original = NativeImage.read(new ByteArrayInputStream(imageData));

            NativeImage resized = new NativeImage(original.format(), 128, 128, false);
            original.resizeSubRectTo(0, 0, original.getWidth(), original.getHeight(), resized);
            original.close();

            Identifier texId = Identifier.fromNamespaceAndPath("ravex", "media_cover");
            if (coverTexture != null) coverTexture.close();
            coverTexture = new DynamicTexture(() -> "ravex:media_cover", resized);
            coverId = texId;
            MinecraftWrapper.getWrapper().getTextureManager().register(texId, coverTexture);

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
        int textW = HudRendererUtility.textWidth(text);
        if (textW <= maxWidth) {
            HudRendererUtility.drawText(graphics, text, x, y, color, shadow);
            return;
        }

        String extended = text + "      ";
        int extW = HudRendererUtility.textWidth(extended);

        double speed = 20.0;
        double timeSecs = System.currentTimeMillis() / 1000.0;
        int scrollX = (int) ((timeSecs * speed) % extW);

        ravex.utility.render.Render2DUtility.pushScissor(graphics, x, y - 1, maxWidth, HudRendererUtility.fontHeight() + 3);

        HudRendererUtility.drawText(graphics, extended, x - scrollX, y, color, shadow);
        if (x - scrollX + extW < x + maxWidth) {
            HudRendererUtility.drawText(graphics, extended, x - scrollX + extW, y, color, shadow);
        }

        ravex.utility.render.Render2DUtility.popScissor(graphics);
    }
    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Modules.enabled(Hud.class)) return;

        String title = cachedTitle;
        if (title.isEmpty()) return;

        int activeColor = ColorUtility.getActiveColor();
        int bx = x;
        int by = y;
        int pw = 160;
        int ph = 42;

        width = pw;
        height = ph;

        long pos = cachedPosition;
        if (cachedPlaying && cachedLength > 0) {
            long elapsed = (System.nanoTime() - lastPositionTime) / 1000;
            pos = Math.min(pos + elapsed, cachedLength);
        }
        displayPosition = pos;

        boolean hasArt = coverTexture != null && coverId != null;

        int bgColor = 0x800C0C0C;
        int borderColor = ravex.gui.clickgui.ColorUtility.withAlpha(activeColor, 75);
        ravex.utility.render.Render2DUtility.drawRoundedRectWithBorder(graphics, bx, by, pw, ph, 5, bgColor, borderColor, 1);

        if (hasArt) {
            graphics.blit(coverId, bx + 5, by + 5, bx + 37, by + 37, 0.0F, 1.0F, 0.0F, 1.0F);
            ravex.utility.render.Render2DUtility.drawRoundBorder(graphics, bx + 5, by + 5, 32, 32, 1, 1, 0x30FFFFFF);
        } else {
            int placeholderBg = 0x15FFFFFF;
            ravex.utility.render.Render2DUtility.drawRound(graphics, bx + 5, by + 5, 32, 32, 4, placeholderBg);
            HudRendererUtility.drawIcon(graphics, ICON, bx + 5 + (32 - IS) / 2, by + 5 + (32 - IS) / 2, activeColor);
        }

        int textX = bx + 42;
        int maxTextWidth = 160 - 42 - 5;

        String titleStr = (cachedPlaying ? "\u25B6 " : "\u23F8 ") + title;
        String artistStr = cachedPlaying && !cachedArtist.isEmpty() ? cachedArtist : "";
        String timeStr = "";
        if (cachedLength > 0) {
            timeStr = formatTime(pos) + " / " + formatTime(cachedLength);
        } else {
            timeStr = "0:00 / 0:00";
        }

        drawScrollingText(graphics, titleStr, textX, by + 4, maxTextWidth, cachedPlaying ? 0xFFE0E0FF : 0xFF8080A0, true);

        if (!artistStr.isEmpty()) {
            drawScrollingText(graphics, artistStr, textX, by + 13, maxTextWidth, 0xFF80809A, false);
            HudRendererUtility.drawText(graphics, timeStr, textX, by + 22, 0xFF606080, false);
        } else {
            HudRendererUtility.drawText(graphics, timeStr, textX, by + 16, 0xFF606080, false);
        }

        if (cachedLength > 0) {
            float progress = Math.min(1f, (float) pos / (float) cachedLength);
            int barW = pw - 2;
            int filled = (int) (barW * progress);

            graphics.fill(bx + 1, by + ph - 2, bx + pw - 1, by + ph - 1, 0x15FFFFFF);
            if (filled > 0) {
                graphics.fill(bx + 1, by + ph - 2, bx + 1 + filled, by + ph - 1, activeColor);
            }
        }
    }






    

    @Override
    public int getX() { return x; }
    @Override
    public void setX(int x) { this.x = x; }
    @Override
    public int getY() { return y; }
    @Override
    public void setY(int y) { this.y = y; }
    @Override
    public int getWidth() { return width; }
    @Override
    public void setWidth(int w) { this.width = w; }
    @Override
    public int getHeight() { return height; }
    @Override
    public void setHeight(int h) { this.height = h; }

    public boolean isHud() {
        return hud;
    }
}