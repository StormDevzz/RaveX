package ravex.modules.hud;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import ravex.gui.clickgui.ColorUtility;
import ravex.modules.Module;
import ravex.modules.client.Hud;
import ravex.utility.render.HudRenderer;
import ravex.utility.render.TextureLoader;
import ravex.manager.ModuleManager;
import ravex.utility.system.SystemUtility;
public class NowPlayingHud extends Module {
    private static final Identifier ICON = TextureLoader.HUD_MEDIA_WHITE;
    private static final int IS = HudRenderer.getIconSize();
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
    private NowPlayingHud() {
        super("NowPlaying", 10, 310, 180, 20);
    }
    @Override
    public void render(GuiGraphics graphics, float partialTicks) {
        if (!ModuleManager.get(Hud.class).getEnabled()) return;
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
        String titleStr = (playing ? "\u25B6 " : "\u23F8 ") + displayText;
        String subStr = playing && !artist.isEmpty() ? artist : "";
        int tw = HudRenderer.textWidth(titleStr);
        int sw = HudRenderer.textWidth(subStr);
        int contentW = Math.max(tw, sw);
        int pw = 4 + contentW + pad + coverSize + pad + IS + pad;
        int ph = Math.max(HudRenderer.fontHeight() + (subStr.isEmpty() ? 0 : HudRenderer.fontHeight() + 2), coverSize) + pad * 2;
        setWidth(pw);
        setHeight(ph);
        HudRenderer.drawBackground(graphics, bx, by, pw, ph);
        int textX = bx + 4;
        int textY = by + pad;
        int covY = by + pad + (ph - coverSize - pad * 2) / 2;
        int coverX = bx + 4 + contentW + pad;
        if (coverTexture != null && coverId != null) {
            graphics.blit(coverId, coverX, covY, 0, 0, coverSize, coverSize, coverSize, coverSize);
        } else {
            graphics.fill(coverX, covY, coverX + coverSize, covY + coverSize, 0xFF222244);
            graphics.fill(coverX + coverSize / 2 - 4, covY + coverSize / 2 - 4,
                    coverX + coverSize / 2 + 4, covY + coverSize / 2 + 4, activeColor);
        }
        HudRenderer.drawIcon(graphics, ICON, bx + pw - 4 - IS, by + (ph - IS) / 2, activeColor);
        HudRenderer.drawText(graphics, titleStr, textX, textY, playing ? 0xFFD0D0E0 : 0xFF8080A0, true);
        if (!subStr.isEmpty()) {
            HudRenderer.drawText(graphics, subStr, textX, textY + HudRenderer.fontHeight() + 2, 0xFF707090, false);
        }
    }
    private void triggerQuery() {
        try {
            String raw = SystemUtility.getNowPlaying();
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
    }
    private void downloadCoverAsync(String url) {
        new Thread(() -> {
            try {
                byte[] data = SystemUtility.downloadArt(url);
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

    public static boolean maybeEnabled() {
        return maybeEnabled(NowPlayingHud.class);
    }

    public static NowPlayingHud itz() {
        return ModuleManager.get(NowPlayingHud.class);
    }
}
