package ravex.modules.movement;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Input;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
import net.minecraft.world.phys.Vec3;
=======
import net.minecraft.world.phys.Vec3;
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import java.util.List;
<<<<<<< HEAD
import java.util.Random;

public class TickShift extends Module {
    public final ModeParameter mode = new ModeParameter("Mode", "Motion", List.of("Motion", "Strafe", "Timer", "GrimStrict"));
=======
public class TickShift extends Module {
    public static final TickShift INSTANCE = new TickShift();
    public final ModeParameter mode = new ModeParameter("Mode", "Motion", List.of("Motion", "Strafe", "Timer"));
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final NumberParameter delay = new NumberParameter("Delay", 20.0, 1.0, 200.0, 1.0);
    public final NumberParameter duration = new NumberParameter("Duration", 10.0, 1.0, 100.0, 1.0);
    public final NumberParameter speed = new NumberParameter("Speed", 1.8, 1.0, 5.0, 0.1);
    public final NumberParameter timerSpeed = new NumberParameter("TimerSpeed", 1.5, 1.0, 5.0, 0.1);
<<<<<<< HEAD
    public final NumberParameter grimMaxTicks = new NumberParameter("GrimMaxTicks", 10.0, 2.0, 30.0, 1.0);
    public final NumberParameter grimAccumulation = new NumberParameter("GrimAccumulation", 0.3, 0.05, 1.0, 0.05);
    public final NumberParameter grimSpeed = new NumberParameter("GrimSpeed", 1.15, 1.0, 2.0, 0.01);
    public final NumberParameter grimDelay = new NumberParameter("GrimDelay", 50.0, 5.0, 200.0, 1.0);

    private final Random random = new Random();
    private double idleTicks = 0;
    private int boostTicks = 0;
    private int releaseCounter = 0;

    {
        grimMaxTicks.setVisible(() -> "GrimStrict".equals(mode.getValue()));
        grimAccumulation.setVisible(() -> "GrimStrict".equals(mode.getValue()));
        grimSpeed.setVisible(() -> "GrimStrict".equals(mode.getValue()));
        grimDelay.setVisible(() -> "GrimStrict".equals(mode.getValue()));
    }
=======
    private int idleTicks = 0;
    private int boostTicks = 0;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3

    @Override
    protected void onEnable() {
        idleTicks = 0;
        boostTicks = 0;
<<<<<<< HEAD
        releaseCounter = 0;
    }

=======
    }
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        Input input = mc.player.input.keyPresses;
        boolean moving = input.forward() || input.backward() || input.left() || input.right();
<<<<<<< HEAD

        if ("GrimStrict".equals(mode.getValue())) {
            if (!moving) {
                idleTicks += grimAccumulation.getValue();
                boostTicks = 0;
                return;
            }
            if (boostTicks > 0) {
                releaseCounter++;
                int releaseInterval = 3 + random.nextInt(3);
                if (releaseCounter >= releaseInterval) {
                    releaseCounter = 0;
                    boostTicks--;
                }
            } else if (idleTicks < grimDelay.getValue().intValue()) {
                return;
            } else {
                boostTicks = grimMaxTicks.getValue().intValue();
                idleTicks = 0;
                releaseCounter = 0;
            }
            if (boostTicks == 0) return;
            Vec3 motion = mc.player.getDeltaMovement();
            double mult = grimSpeed.getValue();
            mc.player.setDeltaMovement(motion.x * mult, motion.y, motion.z * mult);
            return;
        }

=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (!moving) {
            idleTicks++;
            boostTicks = 0;
            return;
        }
        if (boostTicks > 0) {
            boostTicks--;
        } else if (idleTicks < delay.getValue().intValue()) {
            return;
        } else {
            boostTicks = duration.getValue().intValue();
            idleTicks = 0;
        }
        if (boostTicks == 0) return;
        Vec3 motion = mc.player.getDeltaMovement();
        String m = mode.getValue();
        if (m.equals("Motion")) {
            double mult = speed.getValue();
            mc.player.setDeltaMovement(motion.x * mult, motion.y, motion.z * mult);
        } else if (m.equals("Strafe")) {
            double yaw = mc.player.getYRot() * Math.PI / 180.0;
            double forward = (input.forward() ? 1 : 0) - (input.backward() ? 1 : 0);
            double strafe = (input.right() ? 1 : 0) - (input.left() ? 1 : 0);
            double cos = Math.cos(yaw);
            double sin = Math.sin(yaw);
            double dx = strafe * cos - forward * sin;
            double dz = forward * cos + strafe * sin;
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len > 0) {
                dx /= len;
                dz /= len;
            }
            double baseSpeed = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
            double mult = speed.getValue();
            double targetSpeed = Math.max(baseSpeed, 0.2873) * mult;
            mc.player.setDeltaMovement(dx * targetSpeed, motion.y, dz * targetSpeed);
        } else if (m.equals("Timer")) {
            double mult = timerSpeed.getValue();
            mc.player.setDeltaMovement(motion.x * mult, motion.y, motion.z * mult);
        }
    }
<<<<<<< HEAD
    public static TickShift itz() {
        return ModuleManager.get(TickShift.class);
    }
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
