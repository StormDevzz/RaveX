package ravex.modules.misc;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.item.Items;
import ravex.RaveX;
import ravex.event.Subscribe;
import ravex.event.network.PacketEvent;
import ravex.utility.nativelib.NativeLibraryUtility;

import ravex.utility.player.SwingUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import ravex.utility.misc.PhysicUtility;
@ModuleInfo(name = "FakePearl", category = "Misc")
public class FakePearl implements ModuleAccess {
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
        if (!ravex.manager.ModuleManager.INSTANCE.getByName("FakePearl").getEnabled() || !event.isSend()) return;
        Packet<?> packet = event.getPacket();
        if (!(packet instanceof ServerboundUseItemPacket usePacket)) return;
        String trg = trigger;
        if ("Right Click".equals(trg) || "Both".equals(trg)) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.getItemInHand(usePacket.getHand()).is(Items.ENDER_PEARL)) {
                event.setCancelled(true);
                throwFakePearl();
                SwingUtility.swing(mc.player, usePacket.getHand());
            }
        }
    }
    public void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            ravex.manager.ModuleManager.INSTANCE.getByName("FakePearl").setEnabled(false);
            return;
        }
        if ("OnEnable".equals(trigger) || "Both".equals(trigger)) {
            throwFakePearl();
            if ("OnEnable".equals(trigger)) {
                ravex.manager.ModuleManager.INSTANCE.getByName("FakePearl").setEnabled(false);
            }
        }
    }
    public void throwFakePearl() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        double speed = velocity;
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
            protected double getDefaultGravity() {
                return gravity;
            }
        };
        pearl.setPos(mc.player.getX(), mc.player.getEyeY() - 0.1, mc.player.getZ());
        pearl.setDeltaMovement(new net.minecraft.world.phys.Vec3(vel[0], vel[1], vel[2]));
        mc.level.addEntity(pearl);
        if (sound) {
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

    public static FakePearl itz() {
        return ravex.manager.ModuleManager.delegate(FakePearl.class);
    }


}