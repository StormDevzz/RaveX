package ravex.modules.render;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import ravex.utility.player.InventoryUtility;
=======
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import java.util.ArrayList;
import java.util.List;
public class ToolTips extends Module {
<<<<<<< HEAD
=======
    public static final ToolTips INSTANCE = new ToolTips();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final BooleanParameter showId = new BooleanParameter("ShowID", true);
    public final BooleanParameter showShulker = new BooleanParameter("ShowShulker", true);
    public final BooleanParameter showFood = new BooleanParameter("ShowFood", true);
    public final BooleanParameter showEnchants = new BooleanParameter("ShowEnchants", true);
    public final NumberParameter maxLines = new NumberParameter("MaxLines", 10, 2, 30, 1);
<<<<<<< HEAD
    public net.minecraft.world.item.ItemStack lastStack = net.minecraft.world.item.ItemStack.EMPTY;
    public int lastX;
    public int lastY;

    public List<Component> getTooltip(net.minecraft.world.item.ItemStack stack) {
=======
    public ItemStack lastStack = ItemStack.EMPTY;
    public int lastX;
    public int lastY;

    public List<Component> getTooltip(ItemStack stack) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        List<Component> lines = new ArrayList<>();
        if (stack.isEmpty()) return lines;
        int max = maxLines.getValue().intValue();
        int count = 0;
        if (showId.getValue()) {
            var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            lines.add(Component.literal("§7ID: §f" + id.toString()));
            count++;
        }
        if (showFood.getValue()) {
<<<<<<< HEAD
            var food = InventoryUtility.getFoodProperties(stack);
=======
            FoodProperties food = stack.get(DataComponents.FOOD);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            if (food != null) {
                if (count < max) {
                    lines.add(Component.literal("§cFood: §f" + food.nutrition() + " hunger, " + String.format("%.1f", food.saturation() * 2.0f) + " sat"));
                    count++;
                }
            }
        }
        if (showEnchants.getValue()) {
<<<<<<< HEAD
            var ench = InventoryUtility.getEnchantments(stack);
=======
            var ench = stack.get(DataComponents.ENCHANTMENTS);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            if (ench != null && ench.entrySet() != null) {
                for (var entry : ench.entrySet()) {
                    if (count >= max) break;
                    String name = entry.getKey().getRegisteredName().replace("minecraft:", "");
                    lines.add(Component.literal("§d" + name + " " + entry.getIntValue()));
                    count++;
                }
            }
        }
        return lines;
    }
<<<<<<< HEAD
    public boolean isShulker(net.minecraft.world.item.ItemStack stack) {
        return InventoryUtility.isShulkerBox(stack);
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(ToolTips.class);
    }

    public static ToolTips itz() {
        return ModuleManager.get(ToolTips.class);
=======
    public boolean isShulker(ItemStack stack) {
        return stack.getItem() == Items.SHULKER_BOX
            || stack.getItem() == Items.WHITE_SHULKER_BOX
            || stack.getItem() == Items.ORANGE_SHULKER_BOX
            || stack.getItem() == Items.MAGENTA_SHULKER_BOX
            || stack.getItem() == Items.LIGHT_BLUE_SHULKER_BOX
            || stack.getItem() == Items.YELLOW_SHULKER_BOX
            || stack.getItem() == Items.LIME_SHULKER_BOX
            || stack.getItem() == Items.PINK_SHULKER_BOX
            || stack.getItem() == Items.GRAY_SHULKER_BOX
            || stack.getItem() == Items.LIGHT_GRAY_SHULKER_BOX
            || stack.getItem() == Items.CYAN_SHULKER_BOX
            || stack.getItem() == Items.PURPLE_SHULKER_BOX
            || stack.getItem() == Items.BLUE_SHULKER_BOX
            || stack.getItem() == Items.BROWN_SHULKER_BOX
            || stack.getItem() == Items.GREEN_SHULKER_BOX
            || stack.getItem() == Items.RED_SHULKER_BOX
            || stack.getItem() == Items.BLACK_SHULKER_BOX;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    }
}
