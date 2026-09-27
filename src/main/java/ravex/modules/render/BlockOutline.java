package ravex.modules.render;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.parameter.ColorParameter;
import ravex.manager.ModuleManager;
import ravex.utility.render.ColorUtility;

@Module(name = "BlockOutline", category = "Render")
public class BlockOutline {
    @Parameter(name = "Mode", modes = {"Thin", "Thick"})
    public String mode = "Thin";
    @Parameter(name = "Color", color = true)
    public int color = 0xFFFFFF55;
    @Parameter(name = "ThemeSync")
    public boolean themeSync = false;
    @Parameter(name = "Filled")
    public boolean filled = true;
    @Parameter(name = "Smooth")
    public boolean smooth = false;
    @Parameter(name = "Thickness", min = 0.5, max = 4.0, step = 0.1)
    public double thickness = 1.0;
    @Parameter(name = "RightBias", min = 0.0, max = 2.0, step = 0.1)
    public double rightBias = 0.6;

    public static boolean vanillaOutlineEnabled = true;
    private boolean lastThemeSync = false;

    private ColorParameter colorParam() {
        var mod = ModuleManager.delegate(BlockOutline.class);
        if (mod == null) return null;
        for (var p : mod.getParameters()) {
            if (p instanceof ColorParameter cp && cp.getName().equals("Color")) return cp;
        }
        return null;
    }

    public int resolveColor() {
        if (themeSync) return ColorUtility.getActiveColor();
        ColorParameter cp = colorParam();
        if (cp != null) return cp.getValue();
        return color;
    }

    public void onTick() {
        ColorParameter cp = colorParam();
        if (cp == null) return;
        if (themeSync != lastThemeSync) {
            cp.setThemeSync(themeSync);
            lastThemeSync = themeSync;
        } else if (cp.isThemeSync() != themeSync) {
            themeSync = cp.isThemeSync();
            lastThemeSync = themeSync;
        }
    }
}
