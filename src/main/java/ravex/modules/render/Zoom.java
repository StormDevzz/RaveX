package ravex.modules.render;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
import net.minecraft.client.Minecraft;
=======
import net.minecraft.client.Minecraft;
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
public class Zoom extends Module {
<<<<<<< HEAD
=======
    public static final Zoom INSTANCE = new Zoom();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final BooleanParameter smooth = new BooleanParameter("Smooth", true);
    public final NumberParameter smoothSpeed = new NumberParameter("SmoothSpeed", 0.15, 0.05, 0.5, 0.05);
    public final NumberParameter defaultZoom = new NumberParameter("DefaultZoom", 30, 5, 90, 5);
    public final BooleanParameter scroll = new BooleanParameter("Scroll", true);
    public final NumberParameter scrollStep = new NumberParameter("ScrollStep", 5, 1, 20, 1);
    public final NumberParameter minFov = new NumberParameter("MinFov", 5, 1, 30, 1);
    public final NumberParameter maxFov = new NumberParameter("MaxFov", 90, 30, 120, 5);
    private double currentFov;
    private double targetFov;
    private double savedFov;

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) {
            savedFov = mc.options.fov().get();
            targetFov = defaultZoom.getValue();
            currentFov = savedFov;
        }
    }
    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null && savedFov > 0) {
            mc.options.fov().set((int) savedFov);
            currentFov = savedFov;
            targetFov = savedFov;
            savedFov = 0;
        }
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (smooth.getValue()) {
            double spd = smoothSpeed.getValue();
            currentFov += (targetFov - currentFov) * spd;
            if (Math.abs(currentFov - targetFov) < 0.1) currentFov = targetFov;
        } else {
            currentFov = targetFov;
        }
        mc.options.fov().set((int) currentFov);
    }
    public void onScroll(int delta) {
        if (!scroll.getValue() || !getEnabled()) return;
        if (delta > 0) {
            targetFov = Math.max(minFov.getValue(), targetFov - scrollStep.getValue());
        } else {
            targetFov = Math.min(maxFov.getValue(), targetFov + scrollStep.getValue());
        }
    }
    public double getCurrentFov() {
        return currentFov;
    }
<<<<<<< HEAD
    public static boolean maybeEnabled() {
        return maybeEnabled(Zoom.class);
    }

    public static Zoom itz() {
        return ModuleManager.get(Zoom.class);
    }
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
