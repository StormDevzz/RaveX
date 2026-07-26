package ravex.modules.hud;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import ravex.gui.clickgui.ColorUtility;

import ravex.modules.client.Hud;
import ravex.utility.interfaces.IClientPacketListener;
import ravex.utility.nativelib.NativeLibraryUtility;
import ravex.utility.render.HudRendererUtility;
import ravex.utility.render.TextureLoaderUtility;

@ModuleInfo(name = "ServerBrandHud", category = "HUD")
public class ServerBrandHud extends ravex.modules.Module {
    public int x;
    public int y;
    public int width;
    public int height;
private static final Identifier ICON = TextureLoaderUtility.HUD_SERVERBRAND_WHITE;
    private static final int IS = HudRendererUtility.getIconSize();
    private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_jni");
    static {
        NATIVE.load();
    }
    private ServerBrandHud() {
        this.x = 10; this.y = 200; this.width = 100; this.height = 26;
    }
    public static native String nativeFormatBrand(String rawBrand);
    public void render(GuiGraphics graphics, float partialTicks) {
        if (!ravex.manager.ModuleManager.delegate(Hud.class).getEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.player.connection == null) return;
        String rawBrand = null;
        if (mc.player.connection instanceof IClientPacketListener) {
            rawBrand = ((IClientPacketListener) mc.player.connection).ravex$getServerBrand();
        }
        if (rawBrand == null || rawBrand.isEmpty()) {
            rawBrand = "Vanilla";
        }
        String displayBrand;
        if (NATIVE.isLoaded()) {
            try {
                displayBrand = nativeFormatBrand(rawBrand);
            } catch (UnsatisfiedLinkError e) {
                displayBrand = "\u00A77" + rawBrand;
            }
        } else {
            displayBrand = "\u00A77" + rawBrand;
        }
        String labelText = "Server Brand: " + displayBrand;
        int tw = ravex.utility.render.FontRenderUtility.getStringWidth(labelText);
        int pw = Math.max(100, 4 + tw + 4 + IS + 4);
        int ph = 26;
        width = pw;
        height = ph;
        int bx = x, by = y;
        HudRendererUtility.drawBackground(graphics, bx, by, pw, ph);
        ravex.utility.render.FontRenderUtility.drawString(graphics, labelText, bx + 4, by + 8, 0xFFFFFFFF, false);
        HudRendererUtility.drawIcon(graphics, ICON, bx + pw - 4 - IS, by + (ph - IS) / 2, ColorUtility.getActiveColor());
    }

    public static ServerBrandHud itz() {
        return ravex.manager.ModuleManager.delegate(ServerBrandHud.class);
    }

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("ServerBrandHud").getEnabled();
    }

    public java.util.List<ravex.parameter.Parameter<?>> getParameters() {
        java.util.List<ravex.parameter.Parameter<?>> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (ravex.parameter.Parameter.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    list.add((ravex.parameter.Parameter<?>) field.get(this));
                } catch (Exception ignored) {}
            }
        }
        return list;
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