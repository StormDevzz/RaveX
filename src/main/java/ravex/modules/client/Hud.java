package ravex.modules.client;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "Hud", category = "Client", enabled = true)
public class Hud {
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

    public void onTick() {
        if (hudEditor) {
            hudEditor = false;
            var mc = MinecraftWrapper.getInstance();
            if (mc.player != null) {
                mc.execute(() -> mc.setScreen(new ravex.gui.hudeditor.HudEditorScreen(null)));
            }
        }
    }






}