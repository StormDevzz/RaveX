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
            player.setSprinting(true);
        } else {
            if (player.input.hasForwardImpulse() && !PlayerUtility.isUsingItem(player) && !PlayerUtility.isSneaking(player)) {
                player.setSprinting(true);
            }
        }
    }
}
