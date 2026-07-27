package ravex.modules.render;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import ravex.utility.player.InventoryUtility;

import java.util.ArrayList;
import java.util.List;
@Module(name = "ToolTips", category = "Render")
public class ToolTips {
    @Parameter(name = "ShowID")
    public boolean showId = false;
    @Parameter(name = "ShowShulker")
    public boolean showShulker = true;
    @Parameter(name = "ShowFood")
    public boolean showFood = false;
    @Parameter(name = "ShowEnchants")
    public boolean showEnchants = false;
    @Parameter(name = "MaxLines", min = 2, max = 30, step = 1)
    public double maxLines = 10;
    public net.minecraft.world.item.ItemStack lastStack = net.minecraft.world.item.ItemStack.EMPTY;
    public int lastX;
    public int lastY;

    public List<Component> getTooltip(net.minecraft.world.item.ItemStack stack) {
        List<Component> lines = new ArrayList<>();
        if (stack.isEmpty()) return lines;
        int max = (int) maxLines;
        int count = 0;
        if (showFood) {
            var food = InventoryUtility.getFoodProperties(stack);
            if (food != null) {
                if (count < max) {
                    lines.add(Component.literal("§cFood: §f" + food.nutrition() + " hunger, " + String.format("%.1f", food.saturation() * 2.0f) + " sat"));
                    count++;
                }
            }
        }
        if (showEnchants) {
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





}