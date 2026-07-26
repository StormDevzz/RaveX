package ravex.modules.render;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import java.util.List;

@ModuleInfo(name = "BlockOutline", category = "Render")
public class BlockOutline implements ModuleAccess {
    @Parameter(name = "Mode", modes = {"Thin", "Thick"})
    public String mode = "Thin";
    @Parameter(name = "Color", color = true)
    public int color = 0xFFFFFF55;
    @Parameter(name = "Filled")
    public boolean filled = true;
    @Parameter(name = "Smooth")
    public boolean smooth = false;
    
    public static boolean vanillaOutlineEnabled = true;

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("BlockOutline").getEnabled();
    }

    public static BlockOutline itz() {
        return ravex.manager.ModuleManager.delegate(BlockOutline.class);
    }


}