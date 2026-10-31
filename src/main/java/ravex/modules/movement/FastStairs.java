package ravex.modules.movement;
import ravex.utility.player.PlayerUtility;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.movement.MoveUtility;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "FastStairs", category = "Movement")
public class FastStairs {
    @Parameter(name = "Mode", modes = {"Vanilla", "NCP"})
    public String mode = "Vanilla";
    @Parameter(name = "Speed", min = 1.0, max = 5.0, step = 0.1, visible = "mode=Vanilla")
    public double speed = 1.5;
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null || mc.getLevel() == null) return;
        if (!player.onClimbable()) return;
        boolean moving = mc.isForwardKeyDown() || mc.isBackKeyDown() || mc.isLeftKeyDown() || mc.isRightKeyDown();
        if (!moving) return;
        double currentY = PlayerUtility.getDeltaMovement().y;
        if (currentY <= 0) return;
        if ("NCP".equals(mode)) {
            // ncp allows half a block climb per tick
            MoveUtility.setMotion(PlayerUtility.getDeltaMovement().x, 0.5, PlayerUtility.getDeltaMovement().z);
        } else {
            double baseSpeed = currentY;
            MoveUtility.setMotion(PlayerUtility.getDeltaMovement().x, baseSpeed * speed, PlayerUtility.getDeltaMovement().z);
        }
    }
}
