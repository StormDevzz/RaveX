package ravex.modules.render;

import org.lwjgl.glfw.GLFW;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.parameter.KeybindParameter;

@Module(name = "Zoom", category = "Render")
public class Zoom {
    public ravex.parameter.Parameter<Integer> bind = new KeybindParameter("Bind", GLFW.GLFW_KEY_UNKNOWN);

    @Parameter(name = "Speed", min = 5, max = 50, step = 1)
    public double speed = 18;

    @Parameter(name = "Fov", min = 5, max = 90, step = 1)
    public double fov = 30;

    @Parameter(name = "ScrollStep", min = 1, max = 20, step = 1)
    public double scrollStep = 5;

    private double currentFov;
    private long lastMs;
    private boolean animating = false;

    public void onEnable() {
        lastMs = 0;
        animating = false;
    }

    public void onDisable() {
        lastMs = 0;
        animating = false;
    }

    public boolean isHeld() {
        var mc = MinecraftWrapper.getWrapper();
        int key = bind.getValue() == null ? 0 : bind.getValue();
        if (key <= 0) return false;
        if (mc.getCurrentScreen() != null) return false;
        return GLFW.glfwGetKey(mc.getWindowHandle(), key) == GLFW.GLFW_PRESS;
    }

    public float applyFov(float original, boolean primary) {
        long now = System.currentTimeMillis();
        if (lastMs == 0) {
            lastMs = now;
            currentFov = original;
        }
        if (!primary) {
            return original;
        }
        double dt = (now - lastMs) / 1000.0;
        lastMs = now;
        if (dt > 0.1) dt = 0.1;
        if (dt < 0.0) dt = 0.0;
        boolean held = isHeld();
        if (held) {
            animating = true;
        }
        if (!animating) {
            currentFov = original;
            return original;
        }
        double target = held ? fov : original;
        double alpha = 1.0 - Math.exp(-dt * speed);
        currentFov += (target - currentFov) * alpha;
        if (Math.abs(target - currentFov) < 0.05) currentFov = target;
        if (!held && Math.abs(currentFov - original) < 0.05) {
            animating = false;
            currentFov = original;
            return original;
        }
        return (float) currentFov;
    }

    public void adjustScroll(double yOffset) {
        if (yOffset > 0) {
            fov = Math.max(5, fov - scrollStep);
        } else if (yOffset < 0) {
            fov = Math.min(90, fov + scrollStep);
        }
    }
}
