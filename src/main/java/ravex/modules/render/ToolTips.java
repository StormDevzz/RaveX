package ravex.modules.render;
import ravex.manager.ModuleManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import ravex.utility.player.InventoryUtility;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import java.util.ArrayList;
import java.util.List;
public class ToolTips extends Module {
    public final BooleanParameter showId = new BooleanParameter("ShowID", false);
    public final BooleanParameter showShulker = new BooleanParameter("ShowShulker", true);
    public final BooleanParameter showFood = new BooleanParameter("ShowFood", false);
    public final BooleanParameter showEnchants = new BooleanParameter("ShowEnchants", false);
    public final NumberParameter maxLines = new NumberParameter("MaxLines", 10, 2, 30, 1);
    public net.minecraft.world.item.ItemStack lastStack = net.minecraft.world.item.ItemStack.EMPTY;
    public int lastX;
    public int lastY;

    public List<Component> getTooltip(net.minecraft.world.item.ItemStack stack) {
        List<Component> lines = new ArrayList<>();
        if (stack.isEmpty()) return lines;
        int max = maxLines.getValue().intValue();
        int count = 0;
        if (showFood.getValue()) {
            var food = InventoryUtility.getFoodProperties(stack);
            if (food != null) {
                if (count < max) {
                    lines.add(Component.literal("§cFood: §f" + food.nutrition() + " hunger, " + String.format("%.1f", food.saturation() * 2.0f) + " sat"));
                    count++;
                }
            }
        }
        if (showEnchants.getValue()) {
            var ench = InventoryUtility.getEnchantments(stack);
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
    public boolean isShulker(net.minecraft.world.item.ItemStack stack) {
        return InventoryUtility.isShulkerBox(stack) || stack.has(net.minecraft.core.component.DataComponents.CONTAINER);
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(ToolTips.class);
    }

    public static ToolTips itz() {
        return ModuleManager.get(ToolTips.class);
    }
}
