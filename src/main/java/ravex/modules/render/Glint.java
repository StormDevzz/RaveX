package ravex.modules.render;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
@Module(name = "Glint", category = "Render")
public class Glint {
    @Parameter(name = "Items")
    public boolean items = true;
    @Parameter(name = "Armor")
    public boolean armor = true;
    @Parameter(name = "Color", color = true)
    public int color = 0xFFFF00FF;






}