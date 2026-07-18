package ravex.modules.movement;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import net.minecraft.client.Minecraft;
public class NoHungerSprint extends Module {
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.player.getFoodData().getFoodLevel() <= 6.0F
            && mc.player.input.hasForwardImpulse()
            && !mc.player.isUsingItem()
            && !mc.player.isShiftKeyDown()) {
            mc.player.setSprinting(true);
        }
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(NoHungerSprint.class);
    }
    public static NoHungerSprint itz() {
        return ModuleManager.get(NoHungerSprint.class);
    }
}
