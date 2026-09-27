package ravex.modules.hud;

import ravex.modules.annotations.HudModule;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import ravex.RaveX;
import ravex.utility.render.ColorUtility;
import ravex.modules.client.Hud;
import ravex.utility.render.HudRendererUtility;
import ravex.utility.render.TextureLoaderUtility;
import ravex.utility.render.Render2DUtility;
import ravex.utility.system.SystemUtility;
import ravex.utility.nativelib.NativeLoader;
import ravex.gui.hudeditor.HudEditorScreen;
import java.io.ByteArrayInputStream;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
import org.jetbrains.annotations.Nullable;

@HudModule("MediaHud")
public class MediaHud extends ravex.modules.Module {
    public static native String nativeGetNowPlaying();
    public static native byte[] nativeDownloadArt(String url);
    public static native boolean nativeIsAvailable();

    private static final Identifier ICON = TextureLoaderUtility.HUD_MEDIA_WHITE;
    private static final int IS = HudRendererUtility.getIconSize();

    private volatile String cachedTitle = "";
    private volatile String cachedArtist = "";
    private volatile String cachedSource = "";
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

    private volatile long lastSuccessfulPollTime = 0;
    private static final long STALE_GRACE_MS = 3000;
    private static final long SEEK_BACK_US = 500000L;
    private static final long SOURCE_STALL_TOLERANCE_US = 750000L;
    private long lastSourcePos;
    private long lastSourcePollNanos;

    private ScheduledExecutorService scheduler;

    private MediaHud() {
        super("MediaHud", 2, 100, 160, 42);
        setX(10);
        setY(310);
        setWidth(160);
        setHeight(42);
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
        cachedSource = "";
        cachedPlaying = false;
        cachedPosition = 0;
        cachedLength = 0;
        lastLoadedKey = "";
        lastPositionTime = 0;
        lastSourcePos = 0;
        lastSourcePollNanos = 0;
        lastSuccessfulPollTime = 0;
    }

    private void startPolling() {
        if (scheduler != null && !scheduler.isShutdown()) return;
        NativeLoader.loadLibrary("ravex_mediaquery");
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "RaveX-MediaQuery");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleWithFixedDelay(this::pollMedia, 0, 1, TimeUnit.SECONDS);
    }

    private void stopPolling() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    private static final java.util.Set<String> NON_MEDIA_WINDOW_TITLES = java.util.Set.of(
        "Visual Studio Code", "Discord", "Slack", "Obsidian", "Notion",
        "Element", "Telegram", "Signal", "WhatsApp", "Microsoft Teams",
        "Mozilla Firefox", "Brave", "Opera", "Vivaldi",
        "OpenCode", "Cursor", "Windsurf", "Figma", "Linear",
        "IntelliJ IDEA", "PyCharm", "WebStorm", "Android Studio"
    );

    private static final java.util.Set<String> ALLOWED_BARE_TITLES = java.util.Set.of(
        "spotify", "spotify free", "spotify premium"
    );

    private static final java.util.Set<String> BARE_SERVICE_NAMES = java.util.Set.of(
        "youtube", "soundcloud", "youtube music", "deezer",
        "bandcamp", "pandora", "tidal", "new tab"
    );

    private String queryNativeOrFallback() {
        if (SystemUtility.isWindows()) {
            String win = SystemUtility.getNowPlaying();
            if (win != null && !win.isEmpty()) {
                return win;
            }
        }
        String raw = null;
        try {
            raw = NowPlayingHud.nativeGetNowPlaying();
        } catch (Throwable ignored) {}
        if (raw == null || raw.isEmpty()) {
            try {
                raw = nativeGetNowPlaying();
            } catch (Throwable ignored) {}
        }
        if (raw != null && !raw.isEmpty() && isNonMediaWindow(raw)) {
            raw = null;
        }
        if (raw == null || raw.isEmpty()) {
            raw = SystemUtility.getNowPlaying();
            return raw;
        }

        String[] parts = raw.split("\\|", 7);
        if (parts.length < 2) return raw;

        long pos = parts.length >= 6 ? parseLongSafe(parts[5]) : 0;
        long len = parts.length >= 7 ? parseLongSafe(parts[6]) : 0;
        String artUrl = parts.length >= 4 ? parts[3].trim() : "";
        String app = parts.length >= 5 ? parts[4].trim() : "";

        if (pos == 0 && len == 0) {
            String smtc = "";
            try {
                smtc = SystemUtility.getNowPlaying();
            } catch (Throwable ignored) {}
            if (smtc != null && !smtc.isEmpty()) {
                String[] sp = smtc.split("\\|", 7);
                if (sp.length >= 7) {
                    long sPos = parseLongSafe(sp[5]);
                    long sLen = parseLongSafe(sp[6]);
                    if (sPos > 0 || sLen > 0) {
                        pos = sPos;
                        len = sLen;
                    }
                    if (artUrl.isEmpty() && sp.length >= 4) {
                        String sArt = sp[3].trim();
                        if (!sArt.isEmpty()) artUrl = sArt;
                    }
                    if (app.isEmpty() && sp.length >= 5) {
                        String sApp = sp[4].trim();
                        if (!sApp.isEmpty()) app = sApp;
                    }
                }
            }
        }

        String status = parts[0];
        String title = parts[1].trim();
        String artist = parts.length >= 3 ? parts[2].trim() : "";
        return status + "|" + title + "|" + artist + "|" + artUrl + "|" + app + "|" + pos + "|" + len;
    }

    private static boolean isNonMediaWindow(String raw) {
        String[] parts = raw.split("\\|", 7);
        if (parts.length < 2) return false;
        String title = parts[1].trim();
        String artist = parts.length >= 3 ? parts[2].trim() : "";
        String lowerTitle = title.toLowerCase(Locale.ROOT);
        for (String blocked : NON_MEDIA_WINDOW_TITLES) {
            String lowerBlocked = blocked.toLowerCase(Locale.ROOT);
            if (lowerTitle.equals(lowerBlocked) ||
                lowerTitle.endsWith(" - " + lowerBlocked) ||
                lowerTitle.endsWith(" | " + lowerBlocked)) {
                return true;
            }
        }
        if (BARE_SERVICE_NAMES.contains(lowerTitle) && artist.isEmpty()) {
            return true;
        }
        boolean hasSeparator = title.contains(" - ") || title.contains(" | ") ||
            title.contains(" by ") || title.contains(" \u2014 ") || title.contains(" \u2013 ");
        if (!hasSeparator && artist.isEmpty() && !ALLOWED_BARE_TITLES.contains(lowerTitle)) {
            return true;
        }
        return false;
    }

    private void pollMedia() {
        try {
            long sampleNanos = System.nanoTime();
            String raw = queryNativeOrFallback();
            if (raw == null || raw.isEmpty()) {
                if (lastSuccessfulPollTime > 0 && System.currentTimeMillis() - lastSuccessfulPollTime < STALE_GRACE_MS) {
                    return;
                }
                clearCache();
                return;
            }

            String[] parts = raw.split("\\|", 7);
            if (parts.length < 2) {
                if (lastSuccessfulPollTime > 0 && System.currentTimeMillis() - lastSuccessfulPollTime < STALE_GRACE_MS) {
                    return;
                }
                clearCache();
                return;
            }
            boolean nowPlaying = "Playing".equals(parts[0]);
            String title = parts[1].trim();
            String artist = parts.length >= 3 ? parts[2].trim() : "";
            String artUrl = parts.length >= 4 ? parts[3].trim() : "";
            String appIcon = parts.length >= 5 ? parts[4].trim() : "";
            long pos = parts.length >= 6 ? parseLongSafe(parts[5]) : 0;
            long len = parts.length >= 7 ? parseLongSafe(parts[6]) : 0;

            if (title.isEmpty()) {
                if (lastSuccessfulPollTime > 0 && System.currentTimeMillis() - lastSuccessfulPollTime < STALE_GRACE_MS) {
                    return;
                }
                clearCache();
                return;
            }

            if (artist.isEmpty()) {
                String[] split = splitArtistFromTitle(title);
                artist = split[0];
                title = split[1];
            }

            boolean sameTrack = title.equals(cachedTitle) && artist.equals(cachedArtist);
            boolean acceptPosition = true;
            if (sameTrack && nowPlaying && cachedPlaying) {
                if (pos == 0L && cachedPosition > 0L) {
                    acceptPosition = false;
                } else if (lastSourcePollNanos > 0L) {
                    long elapsed = (sampleNanos - lastSourcePollNanos) / 1000L;
                    long advance = pos - lastSourcePos;
                    boolean seekBack = advance < -SEEK_BACK_US;
                    boolean sourceStalled = !seekBack && advance < elapsed - SOURCE_STALL_TOLERANCE_US;
                    if (sourceStalled) acceptPosition = false;
                }
            }
            lastSourcePos = pos;
            lastSourcePollNanos = sampleNanos;

            cachedTitle = title;
            cachedArtist = artist;
            cachedSource = appIcon;
            cachedPlaying = nowPlaying;
            if (acceptPosition) {
                cachedPosition = pos;
                lastPositionTime = sampleNanos;
            }
            cachedLength = len;
            lastSuccessfulPollTime = System.currentTimeMillis();

            String key = title + "|" + artist + "|" + artUrl;
            if (!key.isEmpty() && !key.equals(lastLoadedKey)) {
                byte[] imageData = tryDownload(artUrl, appIcon);
                if (imageData != null && imageData.length > 0) {
                    String capturedKey = key;
                    MinecraftWrapper.getWrapper().execute(() -> {
                        if (registerCover(imageData)) {
                            lastLoadedKey = capturedKey;
                        }
                    });
                }
            }
        } catch (Throwable t) {
            clearCache();
        }
    }

    private static String[] splitArtistFromTitle(String title) {
        String[] delimiters = {" - ", " \u2014 ", " \u2013 ", " ~ ", " / "};
        for (String delim : delimiters) {
            int idx = title.lastIndexOf(delim);
            if (idx > 0 && idx < title.length() - delim.length()) {
                String left = title.substring(0, idx).trim();
                String right = title.substring(idx + delim.length()).trim();
                if (!left.isEmpty() && !right.isEmpty()) {
                    return new String[]{left, right};
                }
            }
        }
        return new String[]{"", title};
    }

    @Nullable
    private byte[] tryDownload(String artUrl, String appIcon) {
        if (!artUrl.isEmpty()) {
            if (artUrl.startsWith("file:///")) {
                byte[] data = SystemUtility.downloadArt(artUrl);
                if (data != null && data.length > 0) return data;
            } else {
                byte[] data = null;
                try {
                    data = NowPlayingHud.nativeDownloadArt(artUrl);
                } catch (Throwable ignored) {}
                if (data == null || data.length == 0) {
                    try {
                        data = nativeDownloadArt(artUrl);
                    } catch (Throwable ignored) {}
                }
                if (data == null || data.length == 0) {
                    data = SystemUtility.downloadArt(artUrl);
                }
                if (data != null && data.length > 0) {
                    return data;
                }
            }
        }
        if (!appIcon.isEmpty()) {
            byte[] data = SystemUtility.getAppIcon(appIcon);
            if (data != null && data.length > 0) {
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
            Render2DUtility.setLinearSampler(coverTexture);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private void clearCache() {
        cachedTitle = "";
        cachedArtist = "";
        cachedSource = "";
        cachedPlaying = false;
        cachedPosition = 0;
        cachedLength = 0;
        lastSuccessfulPollTime = 0;
        lastSourcePos = 0;
        lastSourcePollNanos = 0;
    }

    private static long parseLongSafe(String s) {
        try { return Long.parseLong(s.trim()); } catch (Throwable t) { return 0; }
    }

    private static String formatTime(long micros) {
        long secs = Math.max(0, micros / 1000000);
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

        Render2DUtility.pushScissor(graphics, x, y - 1, maxWidth, HudRendererUtility.fontHeight() + 3);
        HudRendererUtility.drawText(graphics, extended, x - scrollX, y, color, shadow);
        if (x - scrollX + extW < x + maxWidth) {
            HudRendererUtility.drawText(graphics, extended, x - scrollX + extW, y, color, shadow);
        }
        Render2DUtility.popScissor(graphics);
    }

    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Modules.enabled(Hud.class)) return;

        boolean inEditor = MinecraftWrapper.getWrapper().getScreen() instanceof HudEditorScreen;
        String title = cachedTitle;
        String artist = cachedArtist;
        String source = cachedSource;
        boolean playing = cachedPlaying;
        long pos = cachedPosition;
        long len = cachedLength;

        if (title.isEmpty()) {
            if (inEditor) {
                title = "Music Track";
                artist = "Artist Name";
                source = "Spotify";
                playing = true;
                pos = 45000000L;
                len = 180000000L;
            } else {
                return;
            }
        }

        int activeColor = ColorUtility.getActiveColor();
        int bx = getX();
        int by = getY();
        int pw = 175;
        int ph = 46;

        setWidth(pw);
        setHeight(ph);

        if (playing && len > 0 && !inEditor) {
            long elapsed = (System.nanoTime() - lastPositionTime) / 1000;
            pos = Math.min(pos + elapsed, len);
        }
        displayPosition = pos;

        boolean hasArt = coverTexture != null && coverId != null;

        int bgColor = 0x880A0A10;
        int borderColor = ColorUtility.withAlpha(activeColor, 85);
        Render2DUtility.drawRoundedRectWithBorder(graphics, bx, by, pw, ph, 5, bgColor, borderColor, 1);

        if (hasArt) {
            graphics.blit(coverId, bx + 5, by + 5, bx + 41, by + 41, 0.0F, 1.0F, 0.0F, 1.0F);
            Render2DUtility.drawRoundBorder(graphics, bx + 5, by + 5, 36, 36, 2, 1, 0x40FFFFFF);
        } else {
            Render2DUtility.drawRound(graphics, bx + 5, by + 5, 36, 36, 18, 0xFF14141E);
            Render2DUtility.drawRound(graphics, bx + 11, by + 11, 24, 24, 12, 0xFF222232);
            Render2DUtility.drawRound(graphics, bx + 17, by + 17, 12, 12, 6, activeColor);
            Render2DUtility.drawRound(graphics, bx + 21, by + 21, 4, 4, 2, 0xFF0A0A12);
        }

        int textX = bx + 48;
        int maxTextWidth = pw - 48 - 6;

        String titleStr = (playing ? "\u25B6 " : "\u23F8 ") + title;
        String artistStr = (!artist.isEmpty()) ? artist : "";
        String timeStr = (len > 0) ? formatTime(pos) + " / " + formatTime(len) : "0:00 / 0:00";
        if (!source.isEmpty()) {
            timeStr = source + " • " + timeStr;
        }

        if (!artistStr.isEmpty()) {
            drawScrollingText(graphics, titleStr, textX, by + 5, maxTextWidth, playing ? 0xFFE0E0FF : 0xFF8080A0, true);
            drawScrollingText(graphics, artistStr, textX, by + 17, maxTextWidth, 0xFFA0A0BA, false);
            if (HudRendererUtility.textWidth(timeStr) > maxTextWidth) {
                drawScrollingText(graphics, timeStr, textX, by + 29, maxTextWidth, 0xFF707090, false);
            } else {
                HudRendererUtility.drawText(graphics, timeStr, textX, by + 29, 0xFF707090, false);
            }
        } else {
            drawScrollingText(graphics, titleStr, textX, by + 9, maxTextWidth, playing ? 0xFFE0E0FF : 0xFF8080A0, true);
            if (HudRendererUtility.textWidth(timeStr) > maxTextWidth) {
                drawScrollingText(graphics, timeStr, textX, by + 24, maxTextWidth, 0xFF707090, false);
            } else {
                HudRendererUtility.drawText(graphics, timeStr, textX, by + 24, 0xFF707090, false);
            }
        }

        if (len > 0) {
            float progress = Math.min(1f, (float) pos / (float) len);
            int barW = pw - 2;
            int filled = (int) (barW * progress);

            Render2DUtility.drawRect(graphics, bx + 1, by + ph - 2, pw - 2, 2, 0x15FFFFFF);
            if (filled > 0) {
                Render2DUtility.drawRect(graphics, bx + 1, by + ph - 2, filled, 2, activeColor);
            }
        }
    }
}
