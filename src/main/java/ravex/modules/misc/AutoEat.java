package ravex.modules.misc;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.food.FoodUtility;
import net.minecraft.network.chat.Component;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "AutoEat", category = "Misc")
public class AutoEat {
    @Parameter(name = "Hunger", min = 1.0, max = 20.0, step = 1.0)
    public double threshold = 15.0;
    @Parameter(name = "BestFood")
    public boolean priority = true;
    @Parameter(name = "Notify")
    public boolean notify = false;
    @Parameter(name = "Mode", modes = {"Normal", "Silent", "Vanilla"})
    public String mode = "Normal";
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null || mc.getLevel() == null) return;
        float hunger = player.getFoodData().getFoodLevel();
        if ("Vanilla".equals(mode)) {
            mc.getOptions().keyUse.setDown(hunger < threshold);
            return;
        }
        if (FoodUtility.INSTANCE.isEating()) {
            FoodUtility.Result result = FoodUtility.INSTANCE.tryEat();
            if (result == FoodUtility.Result.FINISHED && notify) {
                player.displayClientMessage(
                    Component.literal("§7[§cAutoEat§7] §aDone eating"), false);
            }
            return;
        }
        if (hunger >= threshold) return;
        FoodUtility.Result result = FoodUtility.INSTANCE.tryEat();
        if (result == FoodUtility.Result.STARTED && notify) {
            player.displayClientMessage(
                Component.literal("§7[§cAutoEat§7] §aEating (" + (int)hunger + " hunger)"),
                false);
        }
    }
    public void onDisable() {
        if ("Vanilla".equals(mode)) {
            var mc = MinecraftWrapper.getWrapper();
            var options = mc.getOptions();
            if (options != null) options.keyUse.setDown(false);
        }
        FoodUtility.INSTANCE.reset();
    }
}
