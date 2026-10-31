package ravex.modules.movement;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.event.Subscribe;
import ravex.event.client.TickEvent;
import ravex.modules.Modules;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.movement.MoveUtility;
import ravex.mcwrapper.MinecraftWrapper;

// keeps the player from walking off a block edge.
@Module(name = "SafeWalk", category = "Movement")
public class SafeWalk {
    @Parameter(name = "Mode", modes = {"Block", "Slow"})
    public String mode = "Block";
    @Parameter(name = "OnlyOnGround")
    public boolean onlyOnGround = true;

    @Subscribe
    public void onTick(TickEvent.Client event) {
        if (!Modules.enabled(SafeWalk.class)) return;
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null || mc.getLevel() == null) return;
        if (onlyOnGround && !player.onGround()) return;

        var motion = player.getDeltaMovement();
        if (motion.x == 0.0 && motion.z == 0.0) return;
        if (!isEdge(mc, player, motion.x, motion.z)) return;

        if ("Slow".equals(mode)) {
            MoveUtility.setMotion(motion.x * 0.1, motion.y, motion.z * 0.1);
        } else {
            MoveUtility.setMotion(0.0, motion.y, 0.0);
        }
    }

    // the block under the next step has to exist, otherwise the player falls
    private boolean isEdge(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer player,
                           double dx, double dz) {
        var level = mc.getLevel();
        if (level == null) return false;
        var next = BlockUtility.containing(player.getX() + dx, player.getY() - 0.1, player.getZ() + dz);
        return BlockUtility.isAir(level, next);
    }
}
