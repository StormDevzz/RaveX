package ravex.utility.misc.food;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import ravex.manager.HotbarManager;

public class FoodEater {
    public static final FoodEater INSTANCE = new FoodEater();

    private boolean eating = false;
    private int eatTicks = 0;
    private FoodData currentFood = null;

    private FoodEater() {}

    public EatResult tryEat() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return EatResult.FAIL;

        if (isEating()) {
            return continueEating(mc);
        }

        ravex.utility.misc.food.FoodData best = FoodFinder.INSTANCE.findBestFoodForHunger(
            mc.player.getFoodData().getFoodLevel());

        if (best == null) return EatResult.NO_FOOD;
        return startEating(best, mc);
    }

    public EatResult tryEat(int minHunger) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return EatResult.FAIL;

        if (mc.player.getFoodData().getFoodLevel() >= minHunger) {
            return EatResult.NOT_HUNGRY;
        }
        return tryEat();
    }

    private EatResult startEating(FoodData food, Minecraft mc) {
        int currentSlot = mc.player.getInventory().getSelectedSlot();
        if (currentSlot != food.getSlot()) {
            HotbarManager.INSTANCE.swapToSlot(food.getSlot());
        }

        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        eating = true;
        eatTicks = 35;
        currentFood = food;
        return EatResult.STARTED;
    }

    private EatResult continueEating(Minecraft mc) {
        if (--eatTicks <= 0 || !mc.player.isUsingItem()) {
            finishEating(mc);
            return EatResult.FINISHED;
        }
        return EatResult.EATING;
    }

    public void finishEating(Minecraft mc) {
        eating = false;
        eatTicks = 0;
        currentFood = null;
        HotbarManager.INSTANCE.swapBack();
    }

    public void reset() {
        if (eating) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) finishEating(mc);
        }
        eating = false;
        eatTicks = 0;
        currentFood = null;
    }

    public boolean isEating() { return eating; }
    public FoodData getCurrentFood() { return currentFood; }
    public int getEatTicks() { return eatTicks; }

    public enum EatResult {
        STARTED,
        EATING,
        FINISHED,
        NO_FOOD,
        NOT_HUNGRY,
        FAIL
    }
}
