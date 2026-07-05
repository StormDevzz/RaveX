package ravex.modules.hud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import ravex.gui.clickgui.ColorUtility;
import ravex.modules.Module;
import ravex.modules.client.Hud;
import ravex.utility.interfaces.IClientPacketListener;
import ravex.utility.nativelib.NativeLibrary;
public class ServerBrandHud extends Module {
    public static final ServerBrandHud INSTANCE = new ServerBrandHud();
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_jni");
    static {
        NATIVE.load();
    }
    private ServerBrandHud() {
        super("ServerBrand", 10, 200, 100, 26);
    }
    public static native String nativeFormatBrand(String rawBrand);
    @Override
    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Hud.INSTANCE.getEnabled()) return;
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
        int pw = Math.max(100, tw + 16);
        int ph = 26;
        setWidth(pw);
        setHeight(ph);
        int bx = getX(), by = getY();
        ravex.utility.render.HudRenderer.drawPanel(graphics, bx, by, pw, ph, ColorUtility.getActiveColor());
        ravex.utility.render.FontRenderUtility.drawString(graphics, labelText, bx + 8, by + 8, 0xFFFFFFFF, false);
    }
}
