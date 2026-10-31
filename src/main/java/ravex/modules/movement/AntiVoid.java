package ravex.modules.movement;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.movement.MoveUtility;
import ravex.utility.player.PlayerUtility;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.network.NetworkUtility;
import ravex.mcwrapper.MinecraftWrapper;

@Module(name = "AntiVoid", category = "Movement")
public class AntiVoid {
    @Parameter(name = "Mode", modes = {"NCP", "Teleport"})
    public String mode = "NCP";
    // last solid ground, used by Teleport
    private net.minecraft.world.phys.Vec3 lastGroundPos;
    private int teleportCooldown = 0;

    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var p = mc.getPlayer();
        if (p == null || mc.getLevel() == null) return;
        if (p.onGround()) {
            lastGroundPos = p.position();
            teleportCooldown = 0;
            return;
        }
        // do not fight the player right after a rescue
        if (teleportCooldown > 0) {
            teleportCooldown--;
            return;
        }
        if (PlayerUtility.getFallDistance(p) < 3.0) return;
        if (!overVoid(mc, p)) return;

        if ("NCP".equals(mode)) {
            // rescue onto the nearest real block
            var spot = BlockUtility.findNearestStandable(
                mc.getLevel(), p.blockPosition(), p.getX(), p.getY(), p.getZ(), 5);
            if (spot == null) return;
            teleport(p, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
        } else {
            if (lastGroundPos == null) return;
            teleport(p, lastGroundPos.x, lastGroundPos.y, lastGroundPos.z);
        }
        teleportCooldown = 20;
    }

    // tell the server at once or it pulls us back
    private void teleport(net.minecraft.client.player.LocalPlayer p, double x, double y, double z) {
        MoveUtility.setMotion(0, 0, 0);
        p.setPos(x, y, z);
        NetworkUtility.sendMoveRelative(x, y, z, true, p.horizontalCollision);
    }

    private boolean overVoid(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer player) {
        var level = mc.getLevel();
        if (level == null) return false;
        int x = player.blockPosition().getX();
        int z = player.blockPosition().getZ();
        int minY = BlockUtility.getMinY(level);
        for (int y = player.blockPosition().getY(); y > minY; y--) {
            var pos = BlockUtility.pos(x, y, z);
            if (BlockUtility.hasCollision(level, pos)) return false;
        }
        return true;
    }
}
