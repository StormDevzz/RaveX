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
    @Parameter(name = "AccentXColor", color = true)
    public int accentXColor = 0xFF00D2FF;
    @Parameter(name = "Background")
    public boolean background = true;
    @Parameter(name = "CustomText")
    public String customText = "RaveX";
    @Parameter(name = "ShowVersion")
    public boolean showVersion = true;

    private static final Identifier LOGO = Identifier.fromNamespaceAndPath("ravex", "textures/ravexv2");
    private static final int LOGO_W = 1220;
    private static final int LOGO_H = 1396;
    private static boolean logoLoaded = false;

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

    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Modules.enabled(Hud.class)) return;
        if (!logoLoaded) ensureLogo();

        boolean showLogo = !"Text Only".equals(mode);
        boolean showText = !"Logo Only".equals(mode);

        float aspect = (float) LOGO_W / LOGO_H;
        int logoH = 16;
        int logoW = (int) (logoH * aspect);

        String text = (customText != null && !customText.isEmpty()) ? customText : "RaveX";
        String mainPart = text;
        String xPart = "";
        if (text.endsWith("X")) {
            mainPart = text.substring(0, text.length() - 1);
            xPart = "X";
        } else if (text.endsWith("x")) {
            mainPart = text.substring(0, text.length() - 1);
            xPart = "x";
        }

        String verStr = showVersion ? " v" + RaveX.version : "";

        int contentW = 0;
        if (showLogo) contentW += logoW;
        if (showLogo && showText) contentW += 5;
        if (showText) {
            contentW += HudRendererUtility.textWidth(mainPart) + HudRendererUtility.textWidth(xPart) + HudRendererUtility.textWidth(verStr);
        }

        int pw = contentW + (background ? 12 : 4);
        int ph = 22;
        setWidth(pw);
        setHeight(ph);

        int bx = getX(), by = getY();

        if (background) {
            int bgCol = 0x800C0C12;
            int borderCol = ColorUtility.withAlpha(accentXColor, 75);
            Render2DUtility.drawRoundedRectWithBorder(graphics, bx, by, pw, ph, 5, bgCol, borderCol, 1);
        }

        int cx = bx + (background ? 6 : 2);
        if (showLogo) {
            int ly = by + (ph - logoH) / 2;
            graphics.blit(LOGO, cx, ly, cx + logoW, ly + logoH, 0.0f, 1.0f, 0.0f, 1.0f);
            cx += logoW + 5;
        }

        if (showText) {
            int ty = by + (ph - HudRendererUtility.fontHeight()) / 2;
            HudRendererUtility.drawText(graphics, mainPart, cx, ty, textColor, true);
            cx += HudRendererUtility.textWidth(mainPart);

            if (!xPart.isEmpty()) {
                HudRendererUtility.drawText(graphics, xPart, cx, ty, accentXColor, true);
                cx += HudRendererUtility.textWidth(xPart);
            }

            if (!verStr.isEmpty()) {
                HudRendererUtility.drawText(graphics, verStr, cx, ty, 0xFF707088, false);
            }
        }
    }
}
