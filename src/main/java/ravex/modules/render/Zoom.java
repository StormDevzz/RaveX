package ravex.modules.render;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.mcwrapper.MinecraftWrapper;

@ModuleInfo(name = "Zoom", category = "Render")
public class Zoom implements ModuleAccess {
    @Parameter(name = "Smooth")
    public boolean smooth = true;
    @Parameter(name = "SmoothSpeed", min = 0.05, max = 0.5, step = 0.05)
    public double smoothSpeed = 0.15;
    @Parameter(name = "DefaultZoom", min = 5, max = 90, step = 5)
    public double defaultZoom = 30;
    @Parameter(name = "Scroll")
    public boolean scroll = true;
    @Parameter(name = "ScrollStep", min = 1, max = 20, step = 1)
    public double scrollStep = 5;
    @Parameter(name = "MinFov", min = 1, max = 30, step = 1)
    public double minFov = 5;
    @Parameter(name = "MaxFov", min = 30, max = 120, step = 5)
    public double maxFov = 90;
    private double currentFov;
    private double targetFov;
    private double savedFov;
    public void onEnable() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.options != null) {
            savedFov = mc.options.fov().get();
            targetFov = defaultZoom;
            currentFov = savedFov;
        }
    }
    public void onDisable() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.options != null && savedFov > 0) {
            mc.options.fov().set((int) savedFov);
            currentFov = savedFov;
            targetFov = savedFov;
            savedFov = 0;
        }
    }
    public void onTick() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null) return;
        if (smooth) {
            double spd = smoothSpeed;
            currentFov += (targetFov - currentFov) * spd;
            if (Math.abs(currentFov - targetFov) < 0.1) currentFov = targetFov;
        } else {
            currentFov = targetFov;
        }
        mc.options.fov().set((int) currentFov);
    }
    public void onScroll(int delta) {
        if (!scroll || !ravex.manager.ModuleManager.INSTANCE.getByName("Zoom").getEnabled()) return;
        if (delta > 0) {
            targetFov = Math.max(minFov, targetFov - scrollStep);
        } else {
            targetFov = Math.min(maxFov, targetFov + scrollStep);
        }
    }
    public double getCurrentFov() {
        return currentFov;
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Zoom").getEnabled();
    }

    public static Zoom itz() {
        return ravex.manager.ModuleManager.delegate(Zoom.class);
    }


}