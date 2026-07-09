package ravex.modules.hud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
<<<<<<< HEAD
import net.minecraft.resources.Identifier;
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.gui.clickgui.ColorUtility;
import ravex.modules.Module;
import ravex.modules.client.Hud;
import ravex.utility.interfaces.IClientPacketListener;
import ravex.utility.nativelib.NativeLibrary;
<<<<<<< HEAD
import ravex.utility.render.HudRenderer;
import ravex.utility.render.TextureLoader;
import ravex.manager.ModuleManager;
public class ServerBrandHud extends Module {
    private static final Identifier ICON = TextureLoader.HUD_SERVERBRAND_WHITE;
    private static final int IS = HudRenderer.getIconSize();
=======
public class ServerBrandHud extends Module {
    public static final ServerBrandHud INSTANCE = new ServerBrandHud();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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
<<<<<<< HEAD
        if (!ModuleManager.get(Hud.class).getEnabled()) return;
=======
        if (!Hud.INSTANCE.getEnabled()) return;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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
<<<<<<< HEAD
        int pw = Math.max(100, 4 + tw + 4 + IS + 4);
=======
        int pw = Math.max(100, tw + 16);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        int ph = 26;
        setWidth(pw);
        setHeight(ph);
        int bx = getX(), by = getY();
<<<<<<< HEAD
        HudRenderer.drawBackground(graphics, bx, by, pw, ph);
        ravex.utility.render.FontRenderUtility.drawString(graphics, labelText, bx + 4, by + 8, 0xFFFFFFFF, false);
        HudRenderer.drawIcon(graphics, ICON, bx + pw - 4 - IS, by + (ph - IS) / 2, ColorUtility.getActiveColor());
    }

    public static ServerBrandHud itz() {
        return ModuleManager.get(ServerBrandHud.class);
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(ServerBrandHud.class);
=======
        ravex.utility.render.HudRenderer.drawPanel(graphics, bx, by, pw, ph, ColorUtility.getActiveColor());
        ravex.utility.render.FontRenderUtility.drawString(graphics, labelText, bx + 8, by + 8, 0xFFFFFFFF, false);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    }
}
