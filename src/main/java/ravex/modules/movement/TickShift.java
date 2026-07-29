package ravex.modules.movement;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.PhysicUtility;
import ravex.utility.movement.MoveUtility;
import java.util.List;
import java.util.Random;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.utility.player.PlayerUtility;

@Module(name = "TickShift", category = "Movement")
public class TickShift {
    @Parameter(name = "Mode", modes = {"Motion", "Strafe", "Timer", "GrimStrict"})
    public String mode = "Motion";
    @Parameter(name = "Delay", min = 1.0, max = 200.0, step = 1.0)
    public double delay = 20.0;
    @Parameter(name = "Duration", min = 1.0, max = 100.0, step = 1.0)
    public double duration = 10.0;
    @Parameter(name = "Speed", min = 1.0, max = 5.0, step = 0.1)
    public double speed = 1.8;
    @Parameter(name = "TimerSpeed", min = 1.0, max = 5.0, step = 0.1)
    public double timerSpeed = 1.5;
    @Parameter(name = "GrimMaxTicks", min = 2.0, max = 30.0, step = 1.0, visible = "mode=GrimStrict")
    public double grimMaxTicks = 10.0;
    @Parameter(name = "GrimAccumulation", min = 0.05, max = 1.0, step = 0.05, visible = "mode=GrimStrict")
    public double grimAccumulation = 0.3;
    @Parameter(name = "GrimSpeed", min = 1.0, max = 2.0, step = 0.01, visible = "mode=GrimStrict")
    public double grimSpeed = 1.15;
    @Parameter(name = "GrimDelay", min = 5.0, max = 200.0, step = 1.0, visible = "mode=GrimStrict")
    public double grimDelay = 50.0;

    private final Random random = new Random();
    private double idleTicks = 0;
    private int boostTicks = 0;
    private int releaseCounter = 0;

    public void onEnable() {
        idleTicks = 0;
        boostTicks = 0;
        releaseCounter = 0;
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null) return;
        boolean moving = MoveUtility.isMoving();

        if ("GrimStrict".equals(mode)) {
            if (!moving) {
                idleTicks += grimAccumulation;
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
            } else if (idleTicks < (int) grimDelay) {
                return;
            } else {
                boostTicks = (int) grimMaxTicks;
                idleTicks = 0;
                releaseCounter = 0;
            }
            if (boostTicks == 0) return;
            var motion = mc.getPlayerDeltaMovement();
            double mult = grimSpeed;
            MoveUtility.setMotion(motion.x * mult, motion.y, motion.z * mult);
            return;
        }

        if (!moving) {
            idleTicks++;
            boostTicks = 0;
            return;
        }
        if (boostTicks > 0) {
            boostTicks--;
        } else if (idleTicks < (int) delay) {
            return;
        } else {
            boostTicks = (int) duration;
            idleTicks = 0;
        }
        if (boostTicks == 0) return;
        var motion = mc.getPlayerDeltaMovement();
        String m = mode;
        if (m.equals("Motion")) {
            double mult = speed;
            MoveUtility.setMotion(motion.x * mult, motion.y, motion.z * mult);
        } else if (m.equals("Strafe")) {
            double yaw = player.getYRot() * Math.PI / 180.0;
            var input = player.input.keyPresses;
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
            double mult = speed;
            double targetSpeed = Math.max(baseSpeed, 0.2873) * mult;
            MoveUtility.setMotion(dx * targetSpeed, motion.y, dz * targetSpeed);
        } else if (m.equals("Timer")) {
            double mult = timerSpeed;
            MoveUtility.setMotion(motion.x * mult, motion.y, motion.z * mult);
        }
    }
}
