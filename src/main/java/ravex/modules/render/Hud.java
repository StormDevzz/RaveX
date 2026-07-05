package ravex.modules.render;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
public class Hud extends Module {
    public static final Hud INSTANCE = new Hud();
    public final BooleanParameter hudEditor = new BooleanParameter("HudEditor", false);
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
                mc.execute(() -> mc.setScreen(new ravex.gui.clickgui.HudEditorScreen(null)));
            }
        }
    }
}
