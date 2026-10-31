package ravex.modules.movement;
import ravex.utility.player.PlayerUtility;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.movement.MoveUtility;
import ravex.utility.misc.PotionUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
@Module(name = "LongJump", category = "Movement")
public class LongJump {
    @Parameter(name = "Mode", modes = {"Vanilla", "NCP", "Custom"})
    public String mode = "Vanilla";
    @Parameter(name = "Boost", min = 1.0, max = 10.0, step = 0.1, visible = "mode=Vanilla||mode=Custom")
    public double boost = 1.5;
    // classic ncp stages: boost, slow, decay
    @Parameter(name = "JumpBoost", min = 0.3, max = 0.5, step = 0.01, visible = "mode=NCP||mode=Custom")
    public double jumpBoost = 0.42;
    @Parameter(name = "NCPBoost", min = 1.5, max = 3.0, step = 0.01, visible = "mode=NCP||mode=Custom")
    public double ncpBoost = 2.149;
    @Parameter(name = "Slowdown", min = 0.3, max = 1.0, step = 0.01, visible = "mode=NCP||mode=Custom")
    public double slowdown = 0.66;
    @Parameter(name = "Decay", min = 100.0, max = 200.0, step = 1.0, visible = "mode=NCP||mode=Custom")
    public double decay = 159.0;
    @Parameter(name = "JumpDisable", visible = "mode=NCP||mode=Custom")
    public boolean jumpDisable = true;
    @Parameter(name = "MaxDistance", min = 5.0, max = 40.0, step = 1.0, visible = "mode=NCP||mode=Custom")
    public double maxDistance = 10.0;

    public static boolean jumped = false;
    private int stage = 0;
    private double plannedSpeed;
    private double realSpeed;
    private net.minecraft.world.phys.Vec3 startPos;

    public void onEnable() {
        jumped = false;
        stage = 0;
        plannedSpeed = 0;
        realSpeed = 0;
        var player = MinecraftWrapper.getWrapper().getPlayer();
        startPos = player != null ? player.position() : null;
    }

    public void onDisable() {
        jumped = false;
        stage = 0;
        plannedSpeed = 0;
        realSpeed = 0;
        startPos = null;
    }

    private static float getForward(MinecraftWrapper mc) {
        if (mc.getOptions() == null) return 0;
        float f = 0;
        if (mc.getOptions().keyUp.isDown()) f++;
        if (mc.getOptions().keyDown.isDown()) f--;
        return f;
    }

    private static float getStrafe(MinecraftWrapper mc) {
        if (mc.getOptions() == null) return 0;
        float s = 0;
        if (mc.getOptions().keyLeft.isDown()) s++;
        if (mc.getOptions().keyRight.isDown()) s--;
        return s;
    }

    private void resetMachine() {
        stage = 0;
        plannedSpeed = 0;
        realSpeed = 0;
        var player = MinecraftWrapper.getWrapper().getPlayer();
        startPos = player != null ? player.position() : null;
    }

    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null) return;

        if ("Vanilla".equals(mode)) {
            if (PlayerUtility.isOnGround(player)) {
                jumped = false;
            } else if (!jumped) {
                double speed = boost;
                var motion = PlayerUtility.getDeltaMovement(player);
                MoveUtility.setMotion(motion.x * speed, motion.y + 0.05, motion.z * speed);
                jumped = true;
            }
            return;
        }

        float forward = getForward(mc);
        float strafe = getStrafe(mc);
        if (forward == 0 && strafe == 0) {
            resetMachine();
            return;
        }
        if (startPos == null) {
            startPos = player.position();
        }
        if (player.position().distanceToSqr(startPos) > maxDistance * maxDistance) {
            if (jumpDisable) {
                Modules.setEnabled(LongJump.class, false);
            }
            return;
        }

        realSpeed = Math.hypot(player.getX() - player.xo, player.getZ() - player.zo);

        double defSpeed = 0.2873;
        if (PotionUtility.hasSpeed(player)) {
            defSpeed *= 1.0 + 0.2 * (PotionUtility.getSpeedAmplifier(player) + 1);
        }
        if (PotionUtility.hasSlowness(player)) {
            defSpeed /= 1.0 + 0.2 * (PotionUtility.getSlownessAmplifier(player) + 1);
        }
        if ("Custom".equals(mode)) {
            defSpeed *= Math.max(0.1, boost / 1.5);
        }

        switch (stage) {
            case 0 -> {
                plannedSpeed = 1.35 * defSpeed;
                realSpeed = 0;
                stage = 1;
            }
            case 1 -> {
                MoveUtility.setMotion(
                    PlayerUtility.getDeltaMovement(player).x,
                    jumpBoost,
                    PlayerUtility.getDeltaMovement(player).z
                );
                plannedSpeed *= ncpBoost;
                stage = 2;
            }
            case 2 -> {
                plannedSpeed = realSpeed - slowdown * (realSpeed - defSpeed);
                stage = 3;
            }
            default -> {
                if (player.verticalCollision) {
                    if (jumpDisable) {
                        Modules.setEnabled(LongJump.class, false);
                        return;
                    }
                    resetMachine();
                }
                plannedSpeed = realSpeed - realSpeed / decay;
            }
        }

        plannedSpeed = Math.max(defSpeed, plannedSpeed);

        float f = forward;
        float s = strafe;
        if (f != 0 && s != 0) {
            f *= (float) Math.sin(Math.PI / 4);
            s *= (float) Math.cos(Math.PI / 4);
        }
        double yaw = Math.toRadians(player.getYRot());
        double velX = f * plannedSpeed * -Math.sin(yaw) + s * plannedSpeed * Math.cos(yaw);
        double velZ = f * plannedSpeed * Math.cos(yaw) - s * plannedSpeed * -Math.sin(yaw);
        MoveUtility.setMotion(velX, PlayerUtility.getDeltaMovement(player).y, velZ);
    }
}
