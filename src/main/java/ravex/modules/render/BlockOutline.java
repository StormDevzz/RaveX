package ravex.modules.render;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;
public class BlockOutline extends Module {
    public final ColorParameter color = new ColorParameter("Color", 0xFFFFFF55);
    public final NumberParameter width = new NumberParameter("Width", 2.0, 1.0, 5.0, 0.5);
    public final BooleanParameter filled = new BooleanParameter("Filled", false);
    public static boolean vanillaOutlineEnabled = true;

    public static boolean maybeEnabled() {
        return maybeEnabled(BlockOutline.class);
    }

    public static BlockOutline itz() {
        return ModuleManager.get(BlockOutline.class);
    }
}
