package ravex.modules.render;

import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import java.util.List;

public class BlockOutline extends Module {
    public final ModeParameter mode = new ModeParameter("Mode", "Thin", List.of("Thin", "Thick"));
    public final ColorParameter color = new ColorParameter("Color", 0xFFFFFF55);
    public final BooleanParameter filled = new BooleanParameter("Filled", true);
    public final BooleanParameter smooth = new BooleanParameter("Smooth", false);
    
    public static boolean vanillaOutlineEnabled = true;

    public static boolean maybeEnabled() {
        return maybeEnabled(BlockOutline.class);
    }

    public static BlockOutline itz() {
        return ModuleManager.get(BlockOutline.class);
    }
}
