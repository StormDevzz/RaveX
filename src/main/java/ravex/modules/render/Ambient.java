package ravex.modules.render;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
@Module(name = "Ambient", category = "Render")
public class Ambient {
    @Parameter(name = "Color", color = true)
    public int color = 0x1EFFFFFF;
}