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

import java.io.InputStream;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;

@HudModule("WatermarkHud")
public class WatermarkHud extends ravex.modules.Module {
    @Parameter(name = "Color", color = true)
    public int color = 0xFF1E88E5;

private static final Identifier LOGO = Identifier.fromNamespaceAndPath("ravex", "textures/ravexx");
    private static final int LOGO_W = 305;
    private static final int LOGO_H = 349;
    private static boolean logoLoaded = false;

    private static void ensureLogo() {
        if (logoLoaded) return;
        String path = "/assets/ravex/textures/ravexx.png";
        try (InputStream stream = WatermarkHud.class.getResourceAsStream(path)) {
            if (stream == null) {
                RaveX.LOGGER.warn("[WatermarkHud] Logo not found: {}", path);
                return;
            }
            NativeImage image = NativeImage.read(stream);
            DynamicTexture tex = new DynamicTexture(() -> "ravexx", image);
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

        int ac = this.color;

        int bx = getX(), by = getY();
        long now = System.currentTimeMillis();
        float aspect = (float) LOGO_W / LOGO_H;
        int logoH = 22;
        int logoW = (int) (logoH * aspect);
        setWidth(logoW);
        setHeight(logoH);

        int cx = bx + logoW / 2;
        int cy = by + logoH / 2;
        float angle = (float) Math.sin(now * 0.002) * 12f;
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(cx, cy);
        pose.rotate(angle * (float) Math.PI / 180f);
        pose.translate(-cx, -cy);
        graphics.blit(LOGO, bx, by + 1, bx + logoW, by + 1 + logoH, 0.0f, 1.0f, 0.0f, 1.0f);
        pose.popMatrix();
    }
}
