package ravex.modules.hud;
import ravex.modules.annotations.HudModule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import ravex.utility.render.ColorUtility;

import ravex.modules.client.Hud;
import ravex.utility.interfaces.IClientPacketListener;
import ravex.utility.nativelib.NativeLibraryUtility;
import ravex.utility.render.HudRendererUtility;
import ravex.utility.render.TextureLoaderUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;

@HudModule("ServerBrandHud")
public class ServerBrandHud extends ravex.modules.Module {
private static final Identifier ICON = TextureLoaderUtility.HUD_SERVERBRAND_WHITE;
    private static final int IS = HudRendererUtility.getIconSize();
    private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_jni");
    private ServerBrandHud() {
        super("ServerBrandHud", 3, 70, 150, 20);
        setX(10); setY(200); setWidth(100); setHeight(26);
    }
    public static native String nativeFormatBrand(String rawBrand);
    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Modules.enabled(Hud.class)) return;
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null || mc.getPlayer().connection == null) return;
        String rawBrand = null;
        if (mc.getPlayer().connection instanceof IClientPacketListener) {
            rawBrand = ((IClientPacketListener) mc.getPlayer().connection).ravex$getServerBrand();
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
        setWidth(pw);
        setHeight(ph);
        int bx = getX(), by = getY();
        HudRendererUtility.drawBackground(graphics, bx, by, pw, ph);
        ravex.utility.render.FontRenderUtility.drawString(graphics, labelText, bx + 4, by + 8, 0xFFFFFFFF, false);
        HudRendererUtility.drawIcon(graphics, ICON, bx + pw - 4 - IS, by + (ph - IS) / 2, ColorUtility.getActiveColor());
    }
}
