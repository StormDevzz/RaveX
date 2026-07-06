package ravex.utility.misc.food;

import net.minecraft.world.food.FoodProperties;

public class FoodData {
    private final int slot;
    private final String itemName;
    private final int nutrition;
    private final float saturation;
    private final boolean canAlwaysEat;

    public FoodData(int slot, String itemName, int nutrition, float saturation, boolean canAlwaysEat) {
        this.slot = slot;
        this.itemName = itemName;
        this.nutrition = nutrition;
        this.saturation = saturation;
        this.canAlwaysEat = canAlwaysEat;
    }

    public static FoodData fromSlot(int slot, String itemName, FoodProperties props) {
        return new FoodData(slot, itemName,
            props != null ? props.nutrition() : 0,
            props != null ? props.saturation() : 0,
            props != null && props.canAlwaysEat());
    }

    public int getSlot() { return slot; }
    public String getItemName() { return itemName; }
    public int getNutrition() { return nutrition; }
    public float getSaturation() { return saturation; }
    public boolean canAlwaysEat() { return canAlwaysEat; }

    public float getTotalValue() {
        return nutrition + saturation;
    }

    public int getHungerRestored() {
        return nutrition;
    }
}
