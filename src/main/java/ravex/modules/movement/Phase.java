package ravex.modules.movement;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.item.Items;
import ravex.event.Subscribe;
import ravex.event.network.PacketEvent;
import ravex.utility.movement.MoveUtility;
import ravex.utility.network.NetworkUtility;
import ravex.utility.nativelib.NativeLibraryUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
@Module(name = "Phase", category = "Movement")
public class Phase {
    @Parameter(name = "Mode", modes = {"Positive1", "Positive2"})
    public String mode = "Positive1";
    @Parameter(name = "Distance", min = 0.5, max = 4.0, step = 0.1)
    public double distance = 2.0;
    private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_phase");
    static {
        NATIVE.load();
    }

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!Modules.enabled(Phase.class) || !event.isSend()) return;
        Packet<?> packet = event.getPacket();
        if (packet instanceof ServerboundUseItemPacket usePacket) {
            var mc = MinecraftWrapper.getWrapper();
            var player = mc.getPlayer();
            if (player != null && player.getItemInHand(usePacket.getHand()).is(Items.ENDER_PEARL)) {
                clip();
            }
        }
    }

    public void clip() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null) return;
        double[] offset = new double[3];
        if (NATIVE.isLoaded()) {
            try {
                nativeCalculateOffset(player.getYRot(), player.getXRot(), distance, offset);
            } catch (UnsatisfiedLinkError | Exception e) {
                javaCalculateOffset(player.getYRot(), player.getXRot(), distance, offset);
            }
        } else {
            javaCalculateOffset(player.getYRot(), player.getXRot(), distance, offset);
        }
        double targetX = player.getX() + offset[0];
        double targetY = player.getY() + offset[1];
        double targetZ = player.getZ() + offset[2];
        if ("Positive1".equals(mode)) {
            MoveUtility.setPos(targetX, targetY, targetZ);
            NetworkUtility.sendMoveRelative(targetX, targetY, targetZ, false, player.horizontalCollision);
        } else {
            double steps = 5;
            for (int i = 1; i <= steps; i++) {
                double ratio = (double) i / steps;
                double stepX = player.getX() + offset[0] * ratio;
                double stepY = player.getY() + offset[1] * ratio;
                double stepZ = player.getZ() + offset[2] * ratio;
                NetworkUtility.sendMoveRelative(stepX, stepY, stepZ, false, player.horizontalCollision);
            }
            MoveUtility.setPos(targetX, targetY, targetZ);
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
}
