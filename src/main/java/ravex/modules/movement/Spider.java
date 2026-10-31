package ravex.modules.movement;
import ravex.utility.player.PlayerUtility;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import java.util.Random;
import ravex.utility.network.NetworkUtility;
import ravex.utility.movement.MoveUtility;
import ravex.mcwrapper.MinecraftWrapper;

@Module(name = "Spider", category = "Movement")
public class Spider {
    @Parameter(name = "Mode", modes = {"Vanilla", "NCP", "Custom", "UNCP"})
    public String mode = "Vanilla";
    @Parameter(name = "UNCPMotion", min = 0.1, max = 0.6, step = 0.05, visible = "mode=UNCP")
    public double uncpMotion = 0.2;
    @Parameter(name = "UNCPDelay", min = 1, max = 10, step = 1, visible = "mode=UNCP")
    public int uncpDelay = 2;
    @Parameter(name = "UNCPNoise", min = 0.0, max = 0.01, visible = "mode=UNCP")
    public double uncpNoise = 0.001;
    @Parameter(name = "CustomMotion", min = 0.1, max = 0.6, step = 0.01, visible = "mode=Custom")
    public double customMotion = 0.2;
    @Parameter(name = "CustomDelay", min = 1, max = 20, step = 1, visible = "mode=Custom")
    public int customDelay = 1;
    @Parameter(name = "CustomNoise", min = 0.0, max = 0.01, step = 0.001, visible = "mode=Custom")
    public double customNoise = 0.0;
    @Parameter(name = "CustomYOffset", min = 0.0, max = 0.05, step = 0.005, visible = "mode=Custom")
    public double customYOffset = 0.0;
    @Parameter(name = "CustomOnlyOnGround", visible = "mode=Custom")
    public boolean customOnlyOnGround = false;
    @Parameter(name = "CustomRequireSneak", visible = "mode=Custom")
    public boolean customRequireSneak = false;
    @Parameter(name = "CustomSendPacket", visible = "mode=Custom")
    public boolean customSendPacket = true;
    @Parameter(name = "CustomResetFall", visible = "mode=Custom")
    public boolean customResetFall = false;

    private final Random random = new Random();
    private int uncpTicks = 0;
    private int customTicks = 0;

    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null || mc.getLevel() == null) return;
        if ("Custom".equals(mode)) {
            if (!PlayerUtility.isHorizontalCollision()
                    || (customRequireSneak && (mc.getOptions() == null || !mc.getOptions().keyShift.isDown()))) {
                customTicks = 0;
                return;
            }
            if (customOnlyOnGround && !PlayerUtility.isOnGround()) return;
            if (PlayerUtility.isOnGround()) {
                MoveUtility.setMotion(PlayerUtility.getDeltaMovement().x, customMotion, PlayerUtility.getDeltaMovement().z);
                customTicks = 0;
            }
            customTicks++;
            if (customDelay > 1 && customTicks % customDelay != 0) return;
            if (customResetFall) PlayerUtility.setFallDistance(player, 0);
            if (customSendPacket) {
                double ox = (random.nextDouble() - 0.5) * customNoise * 2;
                double oz = (random.nextDouble() - 0.5) * customNoise * 2;
                NetworkUtility.sendMoveRelative(
                    player.getX() + ox, player.getY() + customYOffset + 0.001, player.getZ() + oz,
                    PlayerUtility.isOnGround(), true
                );
            }
            return;
        }
        if (!"UNCP".equals(mode)) return;

        if (PlayerUtility.isHorizontalCollision() && mc.isForwardKeyDown()) {
            if (PlayerUtility.isOnGround()) {
                MoveUtility.setMotion(PlayerUtility.getDeltaMovement().x, uncpMotion, PlayerUtility.getDeltaMovement().z);
                uncpTicks = 0;
            }

            uncpTicks++;
            double ox = (random.nextDouble() - 0.5) * uncpNoise * 2;
            double oz = (random.nextDouble() - 0.5) * uncpNoise * 2;
            NetworkUtility.sendMoveRelative(
                player.getX() + ox, player.getY() + 0.001, player.getZ() + oz,
                true, true
            );

            if (uncpTicks % uncpDelay == 0) {
                MoveUtility.setMotion(PlayerUtility.getDeltaMovement().x, uncpMotion, PlayerUtility.getDeltaMovement().z);
            }
        }
    }

    public void onDisable() {
        uncpTicks = 0;
        customTicks = 0;
    }
}
