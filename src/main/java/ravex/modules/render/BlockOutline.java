package ravex.modules.render;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.parameter.ColorParameter;
import ravex.manager.ModuleManager;

@Module(name = "BlockOutline", category = "Render")
public class BlockOutline {
    @Parameter(name = "Mode", modes = {"Thin", "Thick"})
    public String mode = "Thin";
    @Parameter(name = "Color", color = true)
    public int color = 0xFF40A9F8;
    @Parameter(name = "Filled")
    public boolean filled = true;
    @Parameter(name = "Smooth")
    public boolean smooth = false;
    @Parameter(name = "Thickness", min = 0.5, max = 4.0, step = 0.1)
    public double thickness = 1.0;
    @Parameter(name = "RightBias", min = 0.0, max = 2.0, step = 0.1)
    public double rightBias = 0.6;

    public static boolean vanillaOutlineEnabled = true;

    private ColorParameter colorParam() {
        var mod = ModuleManager.delegate(BlockOutline.class);
        if (mod == null) return null;
        for (var p : mod.getParameters()) {
            if (p instanceof ColorParameter cp && cp.getName().equals("Color")) return cp;
        }
        return null;
    }

    public int resolveColor() {
        // palette owns theme sync, just read its color
        ColorParameter cp = colorParam();
        if (cp != null) return cp.getValue();
        return color;
    }
}
