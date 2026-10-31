package ravex.modules.movement;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.player.PlayerUtility;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "AutoSprint", category = "Movement")
public class AutoSprint {
    @Parameter(name = "Mode", modes = {"Legit", "Rage"})
    public String mode = "Rage";
    public void onTick() {
        if (ravex.modules.combat.KillAura.shouldBlockSprint()) return;
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null) return;
        if ("Rage".equals(mode)) {
            boolean moving = (mc.getOptions() != null)
                && (mc.getOptions().keyUp.isDown() || mc.getOptions().keyDown.isDown()
                    || mc.getOptions().keyLeft.isDown() || mc.getOptions().keyRight.isDown());
            if (moving) {
                player.setSprinting(true);
            } else if (PlayerUtility.isSprinting(player)) {
                // only the flag here, the mixin handles velocity
                player.setSprinting(false);
            }
        } else {
            if (player.input.hasForwardImpulse() && !PlayerUtility.isUsingItem(player) && !PlayerUtility.isSneaking(player)) {
                player.setSprinting(true);
            }
        }
    }
}
