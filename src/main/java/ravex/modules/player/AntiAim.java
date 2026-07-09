package ravex.modules.player;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
=======
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
import java.util.List;
<<<<<<< HEAD
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.rotation.SilentRotation;
public class AntiAim extends Module {
=======
public class AntiAim extends Module {
    public static final AntiAim INSTANCE = new AntiAim();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final ModeParameter mode = new ModeParameter("YawMode", "Spin", List.of("Spin", "Jitter", "Static"));
    public final ModeParameter pitchMode = new ModeParameter("PitchMode", "Down", List.of("Down", "Up", "Jitter", "None"));
    public final NumberParameter yawSpeed = new NumberParameter("YawSpeed", 30.0, 1.0, 90.0, 1.0);
    public final BooleanParameter silent = new BooleanParameter("Silent", true);
<<<<<<< HEAD
    public static final SilentRotation silentRotation = new SilentRotation();
=======
    private static float silentYaw = 0;
    private static float silentPitch = 0;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    private float spinYaw = 0;
    private int ticks = 0;

    public static float getSilentYaw() {
        return silentRotation.yaw;
    }
    public static float getSilentPitch() {
        return silentRotation.pitch;
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ticks++;
        spinYaw += yawSpeed.getValue().floatValue();
<<<<<<< HEAD
        spinYaw = RotationUtility.normalizeYaw(spinYaw);
=======
        if (spinYaw >= 360f) spinYaw -= 360f;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        float targetYaw = mc.player.getYRot();
        float targetPitch = mc.player.getXRot();
        String yawModeStr = mode.getValue();
        if (yawModeStr.equals("Spin")) {
            targetYaw = spinYaw;
        } else if (yawModeStr.equals("Jitter")) {
            targetYaw = mc.player.getYRot() + (ticks % 2 == 0 ? 90f : -90f);
        } else if (yawModeStr.equals("Static")) {
            targetYaw = mc.player.getYRot() + 180f; 
        }
        String pitchModeStr = pitchMode.getValue();
        if (pitchModeStr.equals("Down")) {
            targetPitch = 90f;
        } else if (pitchModeStr.equals("Up")) {
            targetPitch = -90f;
        } else if (pitchModeStr.equals("Jitter")) {
            targetPitch = ticks % 2 == 0 ? 90f : -90f;
        }
        if (silent.getValue()) {
            silentRotation.set(targetYaw, targetPitch);
        } else {
            mc.player.setYRot(targetYaw);
            mc.player.setXRot(targetPitch);
        }
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(AntiAim.class);
    }
    public static AntiAim itz() {
        return ModuleManager.get(AntiAim.class);
    }
}
