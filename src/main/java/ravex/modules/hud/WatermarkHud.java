package ravex.modules.hud;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import ravex.RaveX;
import ravex.modules.Module;
import ravex.modules.client.Hud;
import ravex.parameter.ColorParameter;
import ravex.mixin.render.AccessorAbstractTexture;
import java.io.InputStream;
import net.minecraft.resources.Identifier;
import ravex.manager.ModuleManager;

public class WatermarkHud extends Module {
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
            try {
                GpuSampler sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
                ((AccessorAbstractTexture) tex).setSampler(sampler);
            } catch (Exception ignored) {}
            Minecraft.getInstance().getTextureManager().register(LOGO, tex);
            logoLoaded = true;
        } catch (Exception e) {
            RaveX.LOGGER.warn("[WatermarkHud] Failed to load logo: {}", e.getMessage());
        }
    }

    private WatermarkHud() {
        super("Watermark", 10, 10, 80, 14);
        addParameter(new ColorParameter("Color", 0xFF1E88E5));
    }

    @Override
    public void render(GuiGraphics graphics, float partialTicks) {
        if (!ModuleManager.get(Hud.class).getEnabled()) return;

        if (!logoLoaded) ensureLogo();

        int ac = 0xFF1E88E5;
        for (var p : getParameters()) {
            if (p instanceof ColorParameter cp && cp.getName().equals("Color")) ac = cp.getValue();
        }

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

    public static boolean maybeEnabled() {
        return maybeEnabled(WatermarkHud.class);
    }

    public static WatermarkHud itz() {
        return ModuleManager.get(WatermarkHud.class);
    }
}
