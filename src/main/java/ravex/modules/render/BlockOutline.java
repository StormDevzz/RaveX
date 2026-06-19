package ravex.modules.render;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;

public class BlockOutline extends Module {
    public static final BlockOutline INSTANCE = new BlockOutline();

    public final ColorParameter color = new ColorParameter("Color", 0xFFFFFF55);
    public final NumberParameter width = new NumberParameter("Width", 2.0, 1.0, 5.0, 0.5);
    public final BooleanParameter filled = new BooleanParameter("Filled", false);

    /** True when the game says a block outline should be shown (looking at a block, GUI closed, etc.) */
    public static boolean vanillaOutlineEnabled = true;

    private BlockOutline() {
        super("BlockOutline", Category.RENDER);
        addParameter(color);
        addParameter(width);
        addParameter(filled);
    }
}
