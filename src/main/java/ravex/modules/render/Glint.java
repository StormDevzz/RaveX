package ravex.modules.render;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
public class Glint extends Module {
    public final BooleanParameter items = new BooleanParameter("Items", true);
    public final BooleanParameter armor = new BooleanParameter("Armor", true);
    public final ColorParameter color = new ColorParameter("Color", 0xFFFF00FF); 

    public static boolean maybeEnabled() {
        return maybeEnabled(Glint.class);
    }

    public static Glint itz() {
        return ModuleManager.get(Glint.class);
    }
}
