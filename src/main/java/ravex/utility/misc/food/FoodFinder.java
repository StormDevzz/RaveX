package ravex.utility.misc.food;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.network.chat.Component;
import ravex.manager.HotbarManager;
import ravex.manager.InventoryManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

public class FoodFinder {
    public static final FoodFinder INSTANCE = new FoodFinder();

    private FoodFinder() {}

    public List<FoodData> findAllFoodInHotbar() {
        List<FoodData> foods = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return foods;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            FoodData data = toFoodData(i, stack);
            if (data != null) foods.add(data);
        }
        return foods;
    }

    public List<FoodData> findAllFoodInInventory() {
        List<FoodData> foods = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return foods;

        for (int i = 0; i < mc.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            FoodData data = toFoodData(i, stack);
            if (data != null) foods.add(data);
        }
        return foods;
    }

    public FoodData findBestFood() {
        return findAllFoodInHotbar().stream()
            .max(Comparator.comparingDouble(FoodData::getTotalValue))
            .orElse(null);
    }

    public FoodData findBestFoodForHunger(int currentHunger) {
        return findAllFoodInHotbar().stream()
            .filter(f -> f.canAlwaysEat() || f.getHungerRestored() + currentHunger <= 20)
            .max(Comparator.comparingDouble(FoodData::getTotalValue))
            .orElse(null);
    }

    public FoodData findFirstFood() {
        List<FoodData> foods = findAllFoodInHotbar();
        return foods.isEmpty() ? null : foods.get(0);
    }

    public boolean hasFoodInHotbar() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        for (int i = 0; i < 9; i++) {
            if (isFood(mc.player.getInventory().getItem(i))) return true;
        }
        return false;
    }

    public static boolean isFood(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.getItem() == Items.SHIELD) return false;
        return stack.get(DataComponents.FOOD) != null;
    }

    public static FoodData toFoodData(int slot, ItemStack stack) {
        if (!isFood(stack)) return null;
        FoodProperties props = (FoodProperties) stack.get(DataComponents.FOOD);
        if (props == null) return null;
        return FoodData.fromSlot(slot, stack.getItem().getName().getString(), props);
    }

    public int countFoodInInventory() {
        return findAllFoodInInventory().size();
    }
}
