package ravex.modules.misc;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.food.FoodUtility;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "AutoEat", category = "Misc")
public class AutoEat {
    @Parameter(name = "Hunger", min = 1.0, max = 20.0, step = 1.0)
    public double threshold = 15.0;
    @Parameter(name = "BestFood")
    public boolean priority = true;
    @Parameter(name = "Gapple")
    public boolean gapple = true;
    @Parameter(name = "MinHealth", min = 1.0, max = 20.0, step = 0.5, visible = "gapple")
    public double minHealth = 10.0;
    @Parameter(name = "Swap", modes = {"None", "Normal", "Silent"})
    public String swapMode = "Normal";
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null || mc.getLevel() == null) return;
        FoodUtility.INSTANCE.setSwapMode(swapMode);
        if (FoodUtility.INSTANCE.isEating()) {
            FoodUtility.INSTANCE.tryEat(priority);
            return;
        }
        if (player.isUsingItem()) return;
        if (gapple && player.getHealth() <= (float) minHealth) {
            FoodUtility.Data apple = FoodUtility.findEnchantedApple();
            if (apple == null) apple = FoodUtility.findApple("Golden");
            if (apple != null) {
                FoodUtility.INSTANCE.tryEatData(apple);
                return;
            }
        }
        float hunger = player.getFoodData().getFoodLevel();
        if (hunger >= threshold) return;
        FoodUtility.INSTANCE.tryEat(priority);
    }
    public void onDisable() {
        FoodUtility.INSTANCE.reset();
    }
}
