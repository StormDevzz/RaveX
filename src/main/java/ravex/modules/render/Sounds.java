package ravex.modules.render;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
@ModuleInfo(name = "Sounds", category = "Render")
public class Sounds implements ModuleAccess {
    @Parameter(name = "Volume", min = 0.0, max = 1.0, step = 0.1)
    public double volume = 1.0;
    private Sounds() {
        
        ravex.manager.ModuleManager.INSTANCE.getByName("Sounds").setEnabled(true);
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Sounds").getEnabled();
    }

    public static Sounds itz() {
        return ravex.manager.ModuleManager.delegate(Sounds.class);
    }


}