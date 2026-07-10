package ravex.modules.render;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;
public class Sounds extends Module {
=======
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;
public class Sounds extends Module {
    public static final Sounds INSTANCE = new Sounds();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final NumberParameter volume = new NumberParameter("Volume", 1.0, 0.0, 1.0, 0.1);
    private Sounds() {
        super("Sounds");
        setEnabled(true);
<<<<<<< HEAD
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(Sounds.class);
    }

    public static Sounds itz() {
        return ModuleManager.get(Sounds.class);
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    }
}
