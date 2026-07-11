package ravex.modules.combat;
import ravex.manager.ModuleManager;
import ravex.utility.player.InventoryUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import ravex.utility.misc.MobUtility;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import java.util.List;
import ravex.utility.nativelib.NativeLibrary;
import ravex.utility.player.rotation.SilentRotation;
public class BowAim extends Module {
    public final NumberParameter range = new NumberParameter("Range", 20.0, 5.0, 40.0, 1.0);
    public final ModeParameter targetType = new ModeParameter("Targets", "Players", List.of("Players", "Mobs", "Both"));
    public final ModeParameter rotate = new ModeParameter("Rotate", "Silent", List.of("Silent", "Normal", "None"));
    public static final SilentRotation silentRotation = new SilentRotation();
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_bowaim");
    static {
        NATIVE.load();
    }

    @Override
    protected void onDisable() {
        silentRotation.hasRotation = false;
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            silentRotation.hasRotation = false;
            return;
        }
        silentRotation.hasRotation = false;
        boolean usingBow = mc.player.isUsingItem() && InventoryUtility.isBow(mc.player.getUseItem());
        if (!usingBow) return;
        int ticksUsed = mc.player.getTicksUsingItem();
        double arrowSpeed = getArrowSpeed(ticksUsed);
        LivingEntity target = findTarget(mc);
        if (target == null) return;
        Vec3 targetVel = target.getDeltaMovement();
        double targetVelX = targetVel != null ? targetVel.x : 0.0;
        double targetVelY = targetVel != null ? targetVel.y : 0.0;
        double targetVelZ = targetVel != null ? targetVel.z : 0.0;
        Vec3 eyePos = mc.player.getEyePosition();
        double[] result;
        if (NATIVE.isLoaded()) {
            result = nativeCalculateBowAim(
                eyePos.x, eyePos.y, eyePos.z,
                target.getX(), target.getY(), target.getZ(),
                targetVelX, targetVelY, targetVelZ,
                target.getBbHeight(),
                arrowSpeed
            );
        } else {
            result = javaCalculateBowAim(
                eyePos.x, eyePos.y, eyePos.z,
                target.getX(), target.getY(), target.getZ(),
                targetVelX, targetVelY, targetVelZ,
                target.getBbHeight(),
                arrowSpeed
            );
        }
        if (result == null || result[0] < 0.5) return;
        float yaw = (float) result[1];
        float pitch = (float) result[2];
        String rMode = rotate.getValue();
        if (rMode.equals("Normal")) {
            mc.player.setYRot(yaw);
            mc.player.setXRot(pitch);
        } else if (rMode.equals("Silent")) {
            silentRotation.set(yaw, pitch);
        }
    }
    private double getArrowSpeed(int ticks) {
        double t = ticks / 20.0;
        double speed = (t * t + t * 2.0) / 3.0;
        if (speed > 1.0) speed = 1.0;
        return speed * 3.0;
    }
    private LivingEntity findTarget(Minecraft mc) {
        LivingEntity closest = null;
        double bestDist = Double.MAX_VALUE;
        double maxDist = range.getValue();
        String filter = targetType.getValue();
        for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof LivingEntity le) || MobUtility.isSelf(le) || MobUtility.isDead(le)) continue;
            if (filter.equals("Players")) {
                if (!MobUtility.isPlayer(le)) continue;
            } else if (filter.equals("Mobs")) {
                if (!MobUtility.isMob(le)) continue;
            }
            double dist = MobUtility.distanceToPlayer(le);
            if (dist <= maxDist && dist < bestDist) {
                bestDist = dist;
                closest = le;
            }
        }
        return closest;
    }
    private double[] javaCalculateBowAim(
        double playerX, double playerY, double playerZ,
        double targetX, double targetY, double targetZ,
        double targetVelX, double targetVelY, double targetVelZ,
        double targetHeight,
        double arrowSpeed
    ) {
        double best_diff = 9999.0;
        double best_yaw = 0;
        double best_pitch = 0;
        int best_ticks = 0;
        double d = 0.99;
        double g = 0.05;
        double targetCenterY = targetY + targetHeight * 0.5;
        for (int t = 1; t <= 40; t++) {
            double predX = targetX + targetVelX * t;
            double predY = targetCenterY + targetVelY * t;
            double predZ = targetZ + targetVelZ * t;
            double dx = predX - playerX;
            double dy = predY - playerY;
            double dz = predZ - playerZ;
            double R = Math.sqrt(dx*dx + dz*dz);
            double Y = dy;
            double St = 0.0;
            double term = 1.0;
            for (int i = 0; i < t; i++) {
                St += term;
                term *= d;
            }
            double Gt = 0.0;
            for (int i = 1; i < t; i++) {
                double sum_d = 0.0;
                double term_d = 1.0;
                for (int j = 0; j < i; j++) {
                    sum_d += term_d;
                    term_d *= d;
                }
                Gt += sum_d;
            }
            double v_h = R / St;
            double v_y = (Y + g * Gt) / St;
            double V_req = Math.sqrt(v_h*v_h + v_y*v_y);
            double diff = Math.abs(V_req - arrowSpeed);
            if (diff < best_diff) {
                best_diff = diff;
                best_ticks = t;
                double yaw_deg = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
                double pitch_deg = -Math.toDegrees(Math.atan2(v_y, v_h));
                best_yaw = yaw_deg;
                best_pitch = pitch_deg;
            }
            if (diff < 0.3) {
                break;
            }
        }
        double[] result = new double[4];
        result[0] = 1.0;
        result[1] = best_yaw;
        result[2] = best_pitch;
        result[3] = best_ticks;
        return result;
    }
    public static boolean hasSilentRotations() {
        return silentRotation.hasRotation;
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(BowAim.class);
    }
    public static BowAim itz() {
        return ModuleManager.get(BowAim.class);
    }
    private static native double[] nativeCalculateBowAim(
        double playerX, double playerY, double playerZ,
        double targetX, double targetY, double targetZ,
        double targetVelX, double targetVelY, double targetVelZ,
        double targetHeight,
        double arrowSpeed
    );
}
