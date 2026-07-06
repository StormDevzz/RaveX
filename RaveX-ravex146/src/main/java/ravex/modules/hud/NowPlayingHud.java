package ravex.modules.hud;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import ravex.gui.clickgui.ColorUtility;
import ravex.modules.HudModule;
import ravex.modules.render.Hud;
import ravex.utility.misc.NativeLoader;
import ravex.utility.render.HudRenderer;

public class NowPlayingHud extends HudModule {
    public static final NowPlayingHud INSTANCE = new NowPlayingHud();

    private static boolean nativeAvailable = false;

    private String title = "";
    private String artist = "";
    private String artUrl = "";
    private boolean playing = false;
    private long lastQuery = 0;

    private DynamicTexture coverTexture;
    private Identifier coverId;
    private String lastLoadedUrl = "";
    private int coverSize = 28;
    private String displayText = "";

    static {
        try {
            nativeAvailable = NativeLoader.loadLibrary("ravex_mediaquery");
            if (nativeAvailable) {
                nativeAvailable = nativeIsAvailable();
            }
        } catch (Throwable t) {
            nativeAvailable = false;
        }
    }

    private NowPlayingHud() {
        super("NowPlaying", 10, 310, 180, 20);
    }

    @Override
    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Hud.INSTANCE.getEnabled()) return;

        long now = System.currentTimeMillis();
        if (now - lastQuery > 2000) {
            lastQuery = now;
            triggerQuery();
        }

        if (displayText.isEmpty()) return;

        int activeColor = ColorUtility.getActiveColor();
        int bx = getX();
        int by = getY();
        int pad = 4;
        int textX = bx + pad + coverSize + pad;
        int textY = by + pad;

        String titleStr = (playing ? "\u25B6 " : "\u23F8 ") + displayText;
        String subStr = playing && !artist.isEmpty() ? artist : "";

        int tw = HudRenderer.textWidth(titleStr);
        int sw = HudRenderer.textWidth(subStr);
        int pw = Math.max(tw, sw) + pad * 2 + coverSize + pad;
        int ph = Math.max(HudRenderer.fontHeight() + (subStr.isEmpty() ? 0 : HudRenderer.fontHeight() + 2), coverSize) + pad * 2;

        HudRenderer.drawPanel(graphics, bx, by, pw, ph, activeColor);

        int covY = by + pad + (ph - coverSize - pad * 2) / 2;
        if (coverTexture != null && coverId != null) {
            graphics.blit(coverId, bx + pad, covY, 0, 0, coverSize, coverSize, coverSize, coverSize);
        } else {
            graphics.fill(bx + pad, covY, bx + pad + coverSize, covY + coverSize, 0xFF222244);
            graphics.fill(bx + pad + coverSize / 2 - 4, covY + coverSize / 2 - 4,
                    bx + pad + coverSize / 2 + 4, covY + coverSize / 2 + 4, activeColor);
        }

        HudRenderer.drawText(graphics, titleStr, textX, textY, playing ? 0xFFD0D0E0 : 0xFF8080A0, true);
        if (!subStr.isEmpty()) {
            HudRenderer.drawText(graphics, subStr, textX, textY + HudRenderer.fontHeight() + 2, 0xFF707090, false);
        }
    }

    private void triggerQuery() {
        if (!nativeAvailable) return;
        Minecraft.getInstance().execute(() -> {
            try {
                String raw = nativeGetNowPlaying();
                if (raw.isEmpty()) {
                    clearTrack();
                    return;
                }
                String[] parts = raw.split("\\|", 4);
                if (parts.length >= 2) {
                    playing = parts[0].equals("Playing");
                    title = parts[1];
                    artist = parts.length >= 3 ? parts[2] : "";
                    String newUrl = parts.length >= 4 ? parts[3] : "";
                    displayText = title;
                    if (!artist.isEmpty()) {
                        displayText = title + " - " + artist;
                    }
                    if (!newUrl.equals(lastLoadedUrl) && !newUrl.isEmpty()) {
                        lastLoadedUrl = newUrl;
                        artUrl = newUrl;
                        downloadCoverAsync(newUrl);
                    }
                }
            } catch (Throwable t) {
                clearTrack();
            }
        });
    }

    private void downloadCoverAsync(String url) {
        new Thread(() -> {
            try {
                byte[] data = nativeDownloadArt(url);
                if (data == null || data.length == 0) return;
                NativeImage img = NativeImage.read(new java.io.ByteArrayInputStream(data));
                int crop = Math.min(img.getWidth(), img.getHeight());
                int ox = (img.getWidth() - crop) / 2;
                int oy = (img.getHeight() - crop) / 2;
                NativeImage out = new NativeImage(coverSize, coverSize, false);
                for (int y = 0; y < coverSize; y++) {
                    for (int x = 0; x < coverSize; x++) {
                        int sx = ox + x * crop / coverSize;
                        int sy = oy + y * crop / coverSize;
                        out.setPixel(x, y, img.getPixel(sx, sy));
                    }
                }
                img.close();
                Identifier texId = Identifier.parse("ravex:nowplaying_cover");
                DynamicTexture dynTex = new DynamicTexture(() -> "ravex:nowplaying_cover", out);
                Minecraft.getInstance().execute(() -> {
                    if (coverTexture != null) coverTexture.close();
                    coverTexture = dynTex;
                    coverId = texId;
                    Minecraft.getInstance().getTextureManager().register(texId, dynTex);
                });
            } catch (Throwable ignored) {
            }
        }, "RaveX-Cover").start();
    }

    private void clearTrack() {
        title = "";
        artist = "";
        artUrl = "";
        lastLoadedUrl = "";
        displayText = "";
    }

    @Override
    protected void onDisable() {
        if (coverTexture != null) {
            coverTexture.close();
            coverTexture = null;
        }
        coverId = null;
        clearTrack();
    }

    private static native String nativeGetNowPlaying();
    private static native boolean nativeIsAvailable();
    private static native byte[] nativeDownloadArt(String url);
}
