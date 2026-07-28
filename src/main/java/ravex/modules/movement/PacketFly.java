package ravex.modules.movement;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.utility.network.NetworkUtility;




@Module(name = "PacketFly", category = "Movement")
public class PacketFly {
    @Parameter(name = "Mode", modes = {"Fast", "Damage", "Setback"})
    public String mode = "Fast";
    @Parameter(name = "Speed", min = 0.05, max = 1.0, step = 0.05)
    public double speed = 0.2;
    @Parameter(name = "Vertical", min = 0.0, max = 1.0, step = 0.05)
    public double vertical = 0.2;
    public void onTick() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null || mc.player.connection == null) return;
        String m = mode;
        double spd = speed;
        double vert = vertical;
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        boolean onGround = mc.player.onGround();
        if (m.equals("Fast")) {
            NetworkUtility.sendMoveRelative(x, y + vert, z, false, onGround);
            NetworkUtility.sendMoveRelative(x, y - 0.05, z, true, onGround);
        } else if (m.equals("Damage")) {
            NetworkUtility.sendMoveRelative(x, y + spd, z, false, onGround);
            NetworkUtility.sendMoveRelative(x, y, z, false, onGround);
        } else if (m.equals("Setback")) {
            NetworkUtility.sendMoveRelative(x, y + 9, z, false, onGround);
            NetworkUtility.sendMoveRelative(x, y + vert, z, false, onGround);
        }
    }



}