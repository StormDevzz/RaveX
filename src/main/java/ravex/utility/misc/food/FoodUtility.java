package ravex.utility.misc.food;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import ravex.utility.player.InventoryUtility;

public class FoodUtility {
    public static class Data {
        private final int slot;
        private final String itemName;
        private final int nutrition;
        private final float saturation;
        private final boolean canAlwaysEat;

        public Data(int slot, String itemName, int nutrition, float saturation, boolean canAlwaysEat) {
            this.slot = slot;
            this.itemName = itemName;
            this.nutrition = nutrition;
            this.saturation = saturation;
            this.canAlwaysEat = canAlwaysEat;
        }

        public static Data fromSlot(int slot, String itemName, FoodProperties props) {
            return new Data(slot, itemName,
                props != null ? props.nutrition() : 0,
                props != null ? props.saturation() : 0,
                props != null && props.canAlwaysEat());
        }

        public int getSlot() { return slot; }
        public String getItemName() { return itemName; }
        public int getNutrition() { return nutrition; }
        public float getSaturation() { return saturation; }
        public boolean canAlwaysEat() { return canAlwaysEat; }
        public float getTotalValue() { return nutrition + saturation; }
        public int getHungerRestored() { return nutrition; }

        public boolean isGoldenApple() {
            return itemName.contains("Golden Apple") || itemName.contains("golden_apple");
        }

        public boolean isEnchantedGoldenApple() {
            return itemName.contains("Enchanted Golden Apple");
        }

        public boolean isAnyGoldenApple() {
            return isGoldenApple() || isEnchantedGoldenApple();
        }

        public boolean isChorusFruit() {
            return itemName.contains("Chorus Fruit") || itemName.contains("chorus_fruit");
        }
    }

    public enum Result {
        STARTED, EATING, FINISHED, NO_FOOD, NOT_HUNGRY, FAIL
    }

    public static final FoodUtility INSTANCE = new FoodUtility();

    private boolean eating = false;
    private int eatTicks = 0;
    private int originalSlot = -1;
    private Data currentFood = null;

    private FoodUtility() {}

    public static boolean isFood(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.getItem() == Items.SHIELD) return false;
        return stack.get(DataComponents.FOOD) != null;
    }

    public static Data toFoodData(int slot, ItemStack stack) {
        if (!isFood(stack)) return null;
        FoodProperties props = stack.get(DataComponents.FOOD);
        if (props == null) return null;
        return Data.fromSlot(slot, stack.getItem().getName().getString(), props);
    }

    public static List<Data> findAllFoodInHotbar() {
        List<Data> foods = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return foods;
        for (int i = 0; i < 9; i++) {
            Data data = toFoodData(i, InventoryUtility.getItem(mc.player, i));
            if (data != null) foods.add(data);
        }
        return foods;
    }

    public static List<Data> findAllFoodInInventory() {
        List<Data> foods = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return foods;
        for (int i = 0; i < InventoryUtility.getContainerSize(mc.player); i++) {
            Data data = toFoodData(i, InventoryUtility.getItem(mc.player, i));
            if (data != null) foods.add(data);
        }
        return foods;
    }

    public static Data findBestFood() {
        return findAllFoodInHotbar().stream()
            .max(Comparator.comparingDouble(Data::getTotalValue))
            .orElse(null);
    }

    public static Data findFood(Predicate<Data> predicate) {
        return findAllFoodInHotbar().stream()
            .filter(predicate)
            .max(Comparator.comparingDouble(Data::getTotalValue))
            .orElse(null);
    }

    public static Data findFoodInInventory(Predicate<Data> predicate) {
        return findAllFoodInInventory().stream()
            .filter(predicate)
            .max(Comparator.comparingDouble(Data::getTotalValue))
            .orElse(null);
    }

    public static Data findBestFoodForHunger(int currentHunger) {
        return findAllFoodInHotbar().stream()
            .filter(f -> f.canAlwaysEat() || f.getHungerRestored() + currentHunger <= 20)
            .max(Comparator.comparingDouble(Data::getTotalValue))
            .orElse(null);
    }

    public static Data findBestFoodForHunger(int currentHunger, Predicate<Data> predicate) {
        return findAllFoodInHotbar().stream()
            .filter(predicate)
            .filter(f -> f.canAlwaysEat() || f.getHungerRestored() + currentHunger <= 20)
            .max(Comparator.comparingDouble(Data::getTotalValue))
            .orElse(null);
    }

    public static Data findFirstFood() {
        List<Data> foods = findAllFoodInHotbar();
        return foods.isEmpty() ? null : foods.get(0);
    }

    public static boolean hasFoodInHotbar() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        for (int i = 0; i < 9; i++) {
            if (isFood(InventoryUtility.getItem(mc.player, i))) return true;
        }
        return false;
    }

    public static int countFoodInInventory() {
        return findAllFoodInInventory().size();
    }

    public static Data findApple(String mode) {
        Predicate<Data> pred = switch (mode) {
            case "Golden" -> Data::isGoldenApple;
            case "Enchanted" -> Data::isEnchantedGoldenApple;
            default -> Data::isAnyGoldenApple;
        };
        return findFood(pred);
    }

    public static Data findEnchantedApple() {
        return findFood(Data::isEnchantedGoldenApple);
    }

    public Result tryEat() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return Result.FAIL;
        if (isEating()) return continueEating(mc);
        Data best = findBestFoodForHunger(mc.player.getFoodData().getFoodLevel());
        if (best == null) return Result.NO_FOOD;
        return startEating(best, mc);
    }

    public Result tryEat(int minHunger) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return Result.FAIL;
        if (mc.player.getFoodData().getFoodLevel() >= minHunger) return Result.NOT_HUNGRY;
        return tryEat();
    }

    private Result startEating(Data food, Minecraft mc) {
        originalSlot = InventoryUtility.getSelectedSlot(mc.player);
        if (originalSlot != food.getSlot()) {
            InventoryUtility.selectSlot(mc.player, food.getSlot());
        }
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        eating = true;
        eatTicks = 35;
        currentFood = food;
        return Result.STARTED;
    }

    private Result continueEating(Minecraft mc) {
        if (--eatTicks <= 0 || !mc.player.isUsingItem()) {
            finishEating(mc);
            return Result.FINISHED;
        }
        return Result.EATING;
    }

    public void finishEating(Minecraft mc) {
        eating = false;
        eatTicks = 0;
        currentFood = null;
        if (originalSlot != -1) {
            InventoryUtility.selectSlot(mc.player, originalSlot);
            originalSlot = -1;
        }
    }

    public void reset() {
        if (eating) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) finishEating(mc);
        }
        eating = false;
        eatTicks = 0;
        originalSlot = -1;
        currentFood = null;
    }

    public boolean isEating() { return eating; }
    public Data getCurrentFood() { return currentFood; }
    public int getEatTicks() { return eatTicks; }
}
