package ravex.modules.render;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import java.util.List;
@ModuleInfo(name = "AspectRatio", category = "Render")
public class AspectRatio implements ModuleAccess {
    @Parameter(name = "Ratio", modes = {"16:9", "16:10", "4:3", "21:9", "Custom"})
    public String ratio = "16:9";
    @Parameter(name = "Width", min = 1, max = 100, step = 1)
    public double customWidth = 16;
    @Parameter(name = "Height", min = 1, max = 100, step = 1)
    public double customHeight = 9;
    private AspectRatio() {
        
    }
    public float getAspectRatio(float original) {
        if (!ravex.manager.ModuleManager.INSTANCE.getByName("AspectRatio").getEnabled()) return original;
        return switch (ratio) {
            case "16:9" -> 16f / 9f;
            case "16:10" -> 16f / 10f;
            case "4:3" -> 4f / 3f;
            case "21:9" -> 21f / 9f;
            case "Custom" -> (float) customWidth / (float) customHeight;
            default -> original;
        };
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("AspectRatio").getEnabled();
    }

    public static AspectRatio itz() {
        return ravex.manager.ModuleManager.delegate(AspectRatio.class);
    }


}