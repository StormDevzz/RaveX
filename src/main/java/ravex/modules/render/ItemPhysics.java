package ravex.modules.render;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;

/**
 * ItemPhysics Module
 * Adds beautiful, high-performance ground-laying physics and spinning animations to dropped item entities.
 */
public class ItemPhysics extends Module {
    public static final ItemPhysics INSTANCE = new ItemPhysics();

    public final NumberParameter scale = new NumberParameter("Scale", 1.0, 0.1, 5.0, 0.1);

    private ItemPhysics() {
        super("ItemPhysics", Category.RENDER);
        addParameter(scale);
    }
}
