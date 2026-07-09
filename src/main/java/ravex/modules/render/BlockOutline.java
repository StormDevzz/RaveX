package ravex.modules.render;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
=======
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;
public class BlockOutline extends Module {
<<<<<<< HEAD
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
=======
    public static final BlockOutline INSTANCE = new BlockOutline();
    public final ColorParameter color = new ColorParameter("Color", 0xFFFFFF55);
    public final NumberParameter width = new NumberParameter("Width", 2.0, 1.0, 5.0, 0.5);
    public final BooleanParameter filled = new BooleanParameter("Filled", false);
    public final BooleanParameter smth = new BooleanParameter("Smth",false);
    public static boolean vanillaOutlineEnabled = true;

>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
