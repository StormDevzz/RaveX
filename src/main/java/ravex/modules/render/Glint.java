package ravex.modules.render;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
@ModuleInfo(name = "Glint", category = "Render")
public class Glint implements ModuleAccess {
    @Parameter(name = "Items")
    public boolean items = true;
    @Parameter(name = "Armor")
    public boolean armor = true;
    @Parameter(name = "Color", color = true)
    public int color = 0xFFFF00FF;

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Glint").getEnabled();
    }

    public static Glint itz() {
        return ravex.manager.ModuleManager.delegate(Glint.class);
    }


}