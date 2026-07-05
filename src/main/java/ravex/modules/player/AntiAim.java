package ravex.modules.player;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
import java.util.List;
public class AntiAim extends Module {
    public static final AntiAim INSTANCE = new AntiAim();
    public final ModeParameter mode = new ModeParameter("YawMode", "Spin", List.of("Spin", "Jitter", "Static"));
    public final ModeParameter pitchMode = new ModeParameter("PitchMode", "Down", List.of("Down", "Up", "Jitter", "None"));
    public final NumberParameter yawSpeed = new NumberParameter("YawSpeed", 30.0, 1.0, 90.0, 1.0);
    public final BooleanParameter silent = new BooleanParameter("Silent", true);
    private static float silentYaw = 0;
    private static float silentPitch = 0;
    private float spinYaw = 0;
    private int ticks = 0;

    public static float getSilentYaw() {
        return silentYaw;
    }
    public static float getSilentPitch() {
        return silentPitch;
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ticks++;
        spinYaw += yawSpeed.getValue().floatValue();
        if (spinYaw >= 360f) spinYaw -= 360f;
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
            silentYaw = targetYaw;
            silentPitch = targetPitch;
        } else {
            mc.player.setYRot(targetYaw);
            mc.player.setXRot(targetPitch);
        }
    }
}
