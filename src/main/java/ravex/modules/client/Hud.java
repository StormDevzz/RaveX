package ravex.modules.client;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
@ModuleInfo(name = "Hud", category = "Client")
public class Hud implements ModuleAccess {
    @Parameter(name = "HudEditor")
    public boolean hudEditor = false;
    @Parameter(name = "EditorOpacity", min = 0, max = 255, step = 1)
    public double editorOpacity = 120;
    @Parameter(name = "EditorBackground")
    public boolean editorBackground = false;
    @Parameter(name = "EditorBlur")
    public boolean editorBlur = true;
    @Parameter(name = "Drag")
    public boolean dragEnabled = false;
    @Parameter(name = "PanelColor", color = true)
    public int panelColor = 0x00000000;
    @Parameter(name = "ShowCounter")
    public boolean showCounter = true;

    public static ravex.modules.Module draggingHud = null;
    public static int dragOffX = 0;
    public static int dragOffY = 0;

    private Hud() {
        
        ravex.manager.ModuleManager.INSTANCE.getByName("Hud").setEnabled(true);
    }
    public void onTick() {
        if (hudEditor) {
            hudEditor = false;
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null) {
                mc.execute(() -> mc.setScreen(new ravex.gui.hudeditor.HudEditorScreen(null)));
            }
        }
    }

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Hud").getEnabled();
    }

    public static Hud itz() {
        return ravex.manager.ModuleManager.delegate(Hud.class);
    }


}