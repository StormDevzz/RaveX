package ravex.modules.movement;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.item.Items;
import ravex.event.Subscribe;
import ravex.event.network.PacketEvent;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.nativelib.NativeLibrary;
import net.minecraft.client.Minecraft;
import java.util.List;
public class Phase extends Module {
    public final ModeParameter mode = new ModeParameter("Mode", "Positive1", List.of("Positive1", "Positive2"));
    public final NumberParameter distance = new NumberParameter("Distance", 2.0, 0.5, 4.0, 0.1);
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_phase");
    static {
        NATIVE.load();
    }

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!getEnabled() || !event.isSend()) return;
        Packet<?> packet = event.getPacket();
        if (packet instanceof ServerboundUseItemPacket usePacket) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.getItemInHand(usePacket.getHand()).is(Items.ENDER_PEARL)) {
                clip();
            }
        }
    }

    public void clip() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;
        double[] offset = new double[3];
        if (NATIVE.isLoaded()) {
            try {
                nativeCalculateOffset(mc.player.getYRot(), mc.player.getXRot(), distance.getValue(), offset);
            } catch (UnsatisfiedLinkError | Exception e) {
                javaCalculateOffset(mc.player.getYRot(), mc.player.getXRot(), distance.getValue(), offset);
            }
        } else {
            javaCalculateOffset(mc.player.getYRot(), mc.player.getXRot(), distance.getValue(), offset);
        }
        double targetX = mc.player.getX() + offset[0];
        double targetY = mc.player.getY() + offset[1];
        double targetZ = mc.player.getZ() + offset[2];
        if ("Positive1".equals(mode.getValue())) {
            mc.player.setPos(targetX, targetY, targetZ);
            mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(targetX, targetY, targetZ, false, mc.player.horizontalCollision));
        } else {
            double steps = 5;
            for (int i = 1; i <= steps; i++) {
                double ratio = (double) i / steps;
                double stepX = mc.player.getX() + offset[0] * ratio;
                double stepY = mc.player.getY() + offset[1] * ratio;
                double stepZ = mc.player.getZ() + offset[2] * ratio;
                mc.getConnection().send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(stepX, stepY, stepZ, false, mc.player.horizontalCollision));
            }
            mc.player.setPos(targetX, targetY, targetZ);
        }
    }
    private void javaCalculateOffset(double yaw, double pitch, double distance, double[] outOffset) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        outOffset[0] = -Math.sin(yawRad) * Math.cos(pitchRad) * distance;
        outOffset[1] = -Math.sin(pitchRad) * distance;
        outOffset[2] = Math.cos(yawRad) * Math.cos(pitchRad) * distance;
    }
    private static native void nativeCalculateOffset(double yaw, double pitch, double distance, double[] outOffset);
    public static boolean maybeEnabled() {
        return maybeEnabled(Phase.class);
    }
    public static Phase itz() {
        return ModuleManager.get(Phase.class);
    }
}
