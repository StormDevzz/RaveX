package ravex.modules.movement;
import ravex.utility.player.PlayerUtility;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.PotionUtility;
import ravex.utility.network.NetworkUtility;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "Step", category = "Movement")
public class Step {
    @Parameter(name = "Mode", modes = {"Vanilla", "Packet", "Grim", "Verus"})
    public String mode = "Vanilla";
    @Parameter(name = "Height", min = 1.0, max = 2.5, step = 0.5)
    public double height = 1.0;
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null) return;
        PotionUtility.setStepHeight(player, height);
        String modeVal = mode;
        boolean hc = PlayerUtility.isHorizontalCollision();
        if (modeVal.equalsIgnoreCase("Packet")) {
            if (hc && PlayerUtility.isOnGround()) {
                double x = player.getX();
                double y = player.getY();
                double z = player.getZ();
                NetworkUtility.sendMoveRelative(x, y + 0.41999998688698, z, false, hc);
                NetworkUtility.sendMoveRelative(x, y + 0.7531999805212, z, false, hc);
            }
        } else if (modeVal.equalsIgnoreCase("Grim")) {
            if (hc && PlayerUtility.isOnGround()) {
                double x = player.getX();
                double y = player.getY();
                double z = player.getZ();
                NetworkUtility.sendMoveRelative(x, y + 0.42, z, false, hc);
                NetworkUtility.sendMoveRelative(x, y + 0.75, z, false, hc);
                NetworkUtility.sendMoveRelative(x, y + 1.0, z, false, hc);
                if (height > 1.0) {
                    NetworkUtility.sendMoveRelative(x, y + 1.5, z, false, hc);
                }
            }
        }
    }
    public void onDisable() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player != null) {
            PotionUtility.resetStepHeight(player);
        }
    }
}
