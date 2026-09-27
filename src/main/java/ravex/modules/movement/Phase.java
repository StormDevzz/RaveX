package ravex.modules.movement;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import net.minecraft.core.BlockPos;
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
    @Parameter(name = "Mode", modes = {"Instant", "Steps", "Pearl", "NCP"})
    public String mode = "Instant";
    @Parameter(name = "Distance", min = 0.5, max = 4.0, step = 0.1)
    public double distance = 2.0;
    @Parameter(name = "NCPStep", min = 0.05, max = 0.5, step = 0.05, visible = "mode=NCP")
    public double ncpStep = 0.15;
    @Parameter(name = "NCPTimeout", min = 500.0, max = 5000.0, step = 100.0, visible = "mode=NCP")
    public double ncpTimeout = 3000.0;
    private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_phase");
    private long pearlThrowMs = 0L;
    private int ncpMoves = 0;

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!Modules.enabled(Phase.class) || !event.isSend()) return;
        Packet<?> packet = event.getPacket();
        if (packet instanceof ServerboundUseItemPacket usePacket) {
            var mc = MinecraftWrapper.getWrapper();
            var player = mc.getPlayer();
            if (player != null && player.getItemInHand(usePacket.getHand()).is(Items.ENDER_PEARL)) {
                String m = mode;
                if ("Instant".equals(m) || "Steps".equals(m)) clip();
                else if ("Pearl".equals(m)) clipPearl();
                else if ("NCP".equals(m)) {
                    pearlThrowMs = System.currentTimeMillis();
                    ncpMoves = 0;
                }
            }
        }
    }

    public void onTick() {
        if (!Modules.enabled(Phase.class) || !"NCP".equals(mode)) return;
        if (pearlThrowMs == 0L) return;
        long now = System.currentTimeMillis();
        if (now - pearlThrowMs > (long) ncpTimeout || ncpMoves >= 40) {
            pearlThrowMs = 0L;
            return;
        }
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null || mc.getLevel() == null) return;
        if (!isEmbedded(mc, player)) return;
        double[] offset = new double[3];
        calculateOffset(player.getYRot(), player.getXRot(), ncpStep, offset);
        double targetX = player.getX() + offset[0];
        double targetY = player.getY() + offset[1];
        double targetZ = player.getZ() + offset[2];
        NetworkUtility.sendMoveRelative(targetX, targetY, targetZ, player.onGround(), player.horizontalCollision);
        MoveUtility.setPos(targetX, targetY, targetZ);
        ncpMoves++;
        if (!isEmbedded(mc, player)) pearlThrowMs = 0L;
    }

    private boolean isEmbedded(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer player) {
        var level = mc.getLevel();
        BlockPos feet = player.blockPosition();
        BlockPos eye = BlockPos.containing(player.getEyePosition());
        return isSolid(level, feet) || isSolid(level, eye);
    }

    private boolean isSolid(net.minecraft.client.multiplayer.ClientLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return !state.isAir() && state.isSolid();
    }

    private void calculateOffset(float yaw, float pitch, double dist, double[] outOffset) {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player != null && NATIVE.isLoaded()) {
            try {
                nativeCalculateOffset(yaw, pitch, dist, outOffset);
                return;
            } catch (UnsatisfiedLinkError | Exception ignored) {}
        }
        javaCalculateOffset(yaw, pitch, dist, outOffset);
    }

    public void clip() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null) return;
        double[] offset = new double[3];
        calculateOffset(player.getYRot(), player.getXRot(), distance, offset);
        double targetX = player.getX() + offset[0];
        double targetY = player.getY() + offset[1];
        double targetZ = player.getZ() + offset[2];
        if ("Instant".equals(mode)) {
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
    public void clipPearl() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null) return;
        double[] offset = new double[3];
        calculateOffset(player.getYRot(), player.getXRot(), distance, offset);
        double targetX = player.getX() + offset[0];
        double targetY = player.getY() + offset[1];
        double targetZ = player.getZ() + offset[2];
        double steps = 8;
        for (int i = 1; i <= steps; i++) {
            double ratio = (double) i / steps;
            double stepX = player.getX() + offset[0] * ratio;
            double stepY = player.getY() + offset[1] * ratio;
            double stepZ = player.getZ() + offset[2] * ratio;
            NetworkUtility.sendMoveRelative(stepX, stepY, stepZ, false, player.horizontalCollision);
        }
        MoveUtility.setPos(targetX, targetY, targetZ);
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
