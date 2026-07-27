package ravex.modules.hud;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import ravex.RaveX;

import ravex.modules.client.Hud;
import ravex.parameter.ColorParameter;
import java.io.InputStream;
import java.lang.reflect.Field;
import net.minecraft.resources.Identifier;
import ravex.mcwrapper.MinecraftWrapper;

@ModuleInfo(name = "WatermarkHud", category = "HUD")
public class WatermarkHud extends ravex.modules.Module {
    @Parameter(name = "Color", color = true)
    public int color = 0xFF1E88E5;

    public int x;
    public int y;
    public int width;
    public int height;
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
                for (Field f : AbstractTexture.class.getDeclaredFields()) {
                    if (GpuSampler.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        f.set(tex, sampler);
                        break;
                    }
                }
            } catch (Exception ignored) {}
            MinecraftWrapper.getInstance().getTextureManager().register(LOGO, tex);
            logoLoaded = true;
        } catch (Exception e) {
            RaveX.LOGGER.warn("[WatermarkHud] Failed to load logo: {}", e.getMessage());
        }
    }

    public void render(GuiGraphics graphics, float partialTicks) {
        if (!ravex.manager.ModuleManager.delegate(Hud.class).getEnabled()) return;

        if (!logoLoaded) ensureLogo();

        int ac = 0xFF1E88E5;
        for (var p : getParameters()) {
            if (p instanceof ColorParameter cp && cp.getName().equals("Color")) ac = cp.getValue();
        }

        int bx = x, by = y;
        long now = System.currentTimeMillis();
        float aspect = (float) LOGO_W / LOGO_H;
        int logoH = 22;
        int logoW = (int) (logoH * aspect);
        width = logoW;
        height = logoH;

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
        return ravex.manager.ModuleManager.INSTANCE.getByName("WatermarkHud").getEnabled();
    }

    public static WatermarkHud itz() {
        return ravex.manager.ModuleManager.delegate(WatermarkHud.class);
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