package ravex.modules.combat;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.PotionUtility;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "Reach", category = "Combat")
public class Reach {
    @Parameter(name = "EntityReach", min = 3.0, max = 6.0, step = 0.1)
    public double entityRange = 4.5;
    @Parameter(name = "BlockReach", min = 4.5, max = 7.0, step = 0.1)
    public double blockRange = 5.5;
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null) return;
        PotionUtility.setEntityInteractionRange(mc.getPlayer(), entityRange);
        PotionUtility.setBlockInteractionRange(mc.getPlayer(), blockRange);
    }
    public void onDisable() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null) return;
        PotionUtility.resetEntityInteractionRange(mc.getPlayer());
        PotionUtility.resetBlockInteractionRange(mc.getPlayer());
    }




}