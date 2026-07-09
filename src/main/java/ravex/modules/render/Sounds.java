package ravex.modules.render;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;
public class Sounds extends Module {
    public final NumberParameter volume = new NumberParameter("Volume", 1.0, 0.0, 1.0, 0.1);
    private Sounds() {
        super("Sounds");
        setEnabled(true); 
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(Sounds.class);
    }

    public static Sounds itz() {
        return ModuleManager.get(Sounds.class);
    }
}
