package ravex.modules.render;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import java.util.List;

@Module(name = "BlockOutline", category = "Render")
public class BlockOutline {
    @Parameter(name = "Mode", modes = {"Thin", "Thick"})
    public String mode = "Thin";
    @Parameter(name = "Color", color = true)
    public int color = 0xFFFFFF55;
    @Parameter(name = "Filled")
    public boolean filled = true;
    @Parameter(name = "Smooth")
    public boolean smooth = false;
    
    public static boolean vanillaOutlineEnabled = true;






}