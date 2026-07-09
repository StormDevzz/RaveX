package ravex.modules.client;
<<<<<<< HEAD
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.manager.ModuleManager;
import ravex.parameter.NumberParameter;
import ravex.parameter.ColorParameter;

public class Hud extends Module {
    public final BooleanParameter hudEditor = new BooleanParameter("HudEditor", false);
    public final NumberParameter editorOpacity = new NumberParameter("EditorOpacity", 120, 0, 255, 1);
    public final BooleanParameter editorBackground = new BooleanParameter("EditorBackground", false);
    public final BooleanParameter editorBlur = new BooleanParameter("EditorBlur", true);
    public final BooleanParameter dragEnabled = new BooleanParameter("Drag", false);
    public final ColorParameter panelColor = new ColorParameter("PanelColor", 0x00000000);

    public static Module draggingHud = null;
    public static int dragOffX = 0;
    public static int dragOffY = 0;

=======
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
public class Hud extends Module {
    public static final Hud INSTANCE = new Hud();
    public final BooleanParameter hudEditor = new BooleanParameter("HudEditor", false);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    private Hud() {
        super("Hud");
        setEnabled(true);
    }
    @Override
    public void onTick() {
        if (hudEditor.getValue()) {
            hudEditor.setValue(false);
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null) {
<<<<<<< HEAD
                mc.execute(() -> mc.setScreen(new ravex.gui.hudeditor.HudEditorScreen(null)));
            }
        }
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(Hud.class);
    }

    public static Hud itz() {
        return ModuleManager.get(Hud.class);
    }
=======
                mc.execute(() -> mc.setScreen(new ravex.gui.clickgui.HudEditorScreen(null)));
            }
        }
    }
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
