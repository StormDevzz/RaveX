package ravex.modules.render;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import java.util.List;
import ravex.modules.Modules;
@Module(name = "AspectRatio", category = "Render")
public class AspectRatio {
    @Parameter(name = "Ratio", modes = {"16:9", "16:10", "4:3", "21:9", "Custom"})
    public String ratio = "16:9";
    @Parameter(name = "Width", min = 1, max = 100, step = 1)
    public double customWidth = 16;
    @Parameter(name = "Height", min = 1, max = 100, step = 1)
    public double customHeight = 9;
    public float getAspectRatio(float original) {
        if (!Modules.enabled(AspectRatio.class)) return original;
        return switch (ratio) {
            case "16:9" -> 16f / 9f;
            case "16:10" -> 16f / 10f;
            case "4:3" -> 4f / 3f;
            case "21:9" -> 21f / 9f;
            case "Custom" -> (float) customWidth / (float) customHeight;
            default -> original;
        };
    }





}