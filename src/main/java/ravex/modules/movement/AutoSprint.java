package ravex.modules.movement;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "AutoSprint", category = "Movement")
public class AutoSprint {
    @Parameter(name = "Mode", modes = {"Legit", "Rage"})
    public String mode = "Rage";
    public void onTick() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null) return;
        if ("Rage".equals(mode)) {
            mc.player.setSprinting(true);
        } else {
            if (mc.player.input.hasForwardImpulse() && !mc.player.isUsingItem() && !mc.player.isShiftKeyDown()) {
                mc.player.setSprinting(true);
            }
        }
    }



}