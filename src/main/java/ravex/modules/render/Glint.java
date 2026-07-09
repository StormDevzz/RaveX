package ravex.modules.render;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
=======
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
public class Glint extends Module {
<<<<<<< HEAD
=======
    public static final Glint INSTANCE = new Glint();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final BooleanParameter items = new BooleanParameter("Items", true);
    public final BooleanParameter armor = new BooleanParameter("Armor", true);
    public final ColorParameter color = new ColorParameter("Color", 0xFFFF00FF); 

<<<<<<< HEAD
    public static boolean maybeEnabled() {
        return maybeEnabled(Glint.class);
    }

    public static Glint itz() {
        return ModuleManager.get(Glint.class);
    }
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
