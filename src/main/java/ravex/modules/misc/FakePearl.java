package ravex.modules.misc;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.item.Items;
import ravex.RaveX;
import ravex.event.Subscribe;
import ravex.event.network.PacketEvent;
import ravex.utility.nativelib.NativeLibraryUtility;
import ravex.utility.player.SwingUtility;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
@Module(name = "FakePearl", category = "Misc")
public class FakePearl {
    @Parameter(name = "Trigger", modes = {"OnEnable", "RightClick", "Both"})
    public String trigger = "OnEnable";
    @Parameter(name = "Velocity", min = 0.5, max = 3.0, step = 0.1)
    public double velocity = 1.5;
    @Parameter(name = "Gravity", min = 0.01, max = 0.1, step = 0.01)
    public double gravity = 0.03;
    @Parameter(name = "Sound")
    public boolean sound = true;
    private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_fakepearl");
    static {
        NATIVE.load();
    }

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!Modules.enabled(FakePearl.class) || !event.isSend()) return;
        Packet<?> packet = event.getPacket();
        if (!(packet instanceof ServerboundUseItemPacket usePacket)) return;
        String trg = trigger;
        if ("Right Click".equals(trg) || "Both".equals(trg)) {
            var mc = MinecraftWrapper.getWrapper();
            var player = mc.getPlayer();
            if (player != null && player.getItemInHand(usePacket.getHand()).is(Items.ENDER_PEARL)) {
                event.setCancelled(true);
                throwFakePearl();
                SwingUtility.swing(player, usePacket.getHand());
            }
        }
    }
    public void onEnable() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) {
            Modules.setEnabled(FakePearl.class, false);
            return;
        }
        if ("OnEnable".equals(trigger) || "Both".equals(trigger)) {
            throwFakePearl();
            if ("OnEnable".equals(trigger)) {
                Modules.setEnabled(FakePearl.class, false);
            }
        }
    }
    public void throwFakePearl() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        var level = mc.getLevel();
        if (player == null || level == null) return;
        double speed = velocity;
        double yaw = player.getYRot();
        double pitch = player.getXRot();
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
        ThrownEnderpearl pearl = new ThrownEnderpearl(level, player, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ENDER_PEARL)) {
            protected double getDefaultGravity() {
                return gravity;
            }
        };
        pearl.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
        pearl.setDeltaMovement(new net.minecraft.world.phys.Vec3(vel[0], vel[1], vel[2]));
        level.addEntity(pearl);
        if (sound) {
            level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.ENDER_PEARL_THROW,
                net.minecraft.sounds.SoundSource.PLAYERS,
                0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F), false);
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
