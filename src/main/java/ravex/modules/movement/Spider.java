package ravex.modules.movement;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import java.util.List;
import java.util.Random;
import ravex.utility.network.NetworkUtility;
import ravex.utility.movement.MoveUtility;
import ravex.mcwrapper.MinecraftWrapper;

@Module(name = "Spider", category = "Movement")
public class Spider {
    @Parameter(name = "Mode", modes = {"Normal", "NCP", "Custom", "UNCP"})
    public String mode = "Normal";
    @Parameter(name = "Motion", min = 0.1, max = 0.6, step = 0.05)
    public double motion = 0.2;
    @Parameter(name = "UNCPMotion", min = 0.1, max = 0.6, step = 0.05, visible = "mode=UNCP")
    public double uncpMotion = 0.2;
    @Parameter(name = "UNCPDelay", min = 1, max = 10, step = 1, visible = "mode=UNCP")
    public int uncpDelay = 2;
    @Parameter(name = "UNCPNoise", min = 0.0, max = 0.01, visible = "mode=UNCP")
    public double uncpNoise = 0.001;

    private final Random random = new Random();
    private int uncpTicks = 0;

    public void onTick() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!"UNCP".equals(mode)) return;

        if (mc.player.horizontalCollision && mc.options.keyUp.isDown()) {
            if (mc.player.onGround()) {
                MoveUtility.setMotion(mc.player.getDeltaMovement().x, uncpMotion, mc.player.getDeltaMovement().z);
                uncpTicks = 0;
            }

            uncpTicks++;
            double ox = (random.nextDouble() - 0.5) * uncpNoise * 2;
            double oz = (random.nextDouble() - 0.5) * uncpNoise * 2;
            NetworkUtility.sendMoveRelative(
                mc.player.getX() + ox, mc.player.getY() + 0.001, mc.player.getZ() + oz,
                true, true
            );

            if (uncpTicks % uncpDelay == 0) {
                MoveUtility.setMotion(mc.player.getDeltaMovement().x, uncpMotion, mc.player.getDeltaMovement().z);
            }
        }
    }

    public void onDisable() {
        uncpTicks = 0;
    }
}
