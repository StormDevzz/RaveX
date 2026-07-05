package ravex.modules.misc;
import ravex.RaveX;
import ravex.utility.nativelib.NativeLibrary;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.phys.Vec3;
public class FakePearl extends Module {
    public static final FakePearl INSTANCE = new FakePearl();
    public final ModeParameter trigger = new ModeParameter("Trigger", "OnEnable", java.util.List.of("OnEnable", "RightClick", "Both"));
    public final NumberParameter velocity = new NumberParameter("Velocity", 1.5, 0.5, 3.0, 0.1);
    public final NumberParameter gravity = new NumberParameter("Gravity", 0.03, 0.01, 0.1, 0.01);
    public final BooleanParameter sound = new BooleanParameter("Sound", true);
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_fakepearl");
    static {
        NATIVE.load();
    }

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            setEnabled(false);
            return;
        }
        if ("OnEnable".equals(trigger.getValue()) || "Both".equals(trigger.getValue())) {
            throwFakePearl();
            if ("OnEnable".equals(trigger.getValue())) {
                setEnabled(false);
            }
        }
    }
    public void throwFakePearl() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        double speed = velocity.getValue();
        double yaw = mc.player.getYRot();
        double pitch = mc.player.getXRot();
        double[] vel = new double[3];
        if (NATIVE.isLoaded()) {
            try {
                nativeCalculateVelocity(yaw, pitch, speed, vel);
            } catch (UnsatisfiedLinkError | Exception e) {
                javaCalculateVelocity(yaw, pitch, speed, vel);
            }
        } else {
            javaCalculateVelocity(yaw, pitch, speed, vel);
        }
        ThrownEnderpearl pearl = new ThrownEnderpearl(mc.level, mc.player, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ENDER_PEARL)) {
            @Override
            protected double getDefaultGravity() {
                return gravity.getValue();
            }
        };
        pearl.setPos(mc.player.getX(), mc.player.getEyeY() - 0.1, mc.player.getZ());
        pearl.setDeltaMovement(new Vec3(vel[0], vel[1], vel[2]));
        mc.level.addEntity(pearl);
        if (sound.getValue()) {
            mc.level.playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                net.minecraft.sounds.SoundEvents.ENDER_PEARL_THROW,
                net.minecraft.sounds.SoundSource.PLAYERS,
                0.5F, 0.4F / (mc.level.getRandom().nextFloat() * 0.4F + 0.8F), false);
        }
    }
    private void javaCalculateVelocity(double yaw, double pitch, double speed, double[] outVel) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        outVel[0] = -Math.sin(yawRad) * Math.cos(pitchRad) * speed;
        outVel[1] = -Math.sin(pitchRad) * speed;
        outVel[2] = Math.cos(yawRad) * Math.cos(pitchRad) * speed;
    }
    private static native void nativeCalculateVelocity(double yaw, double pitch, double speed, double[] outVel);
}
