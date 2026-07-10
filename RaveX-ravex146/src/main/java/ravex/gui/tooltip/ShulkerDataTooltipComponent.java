package ravex.gui.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import java.util.List;

public class ShulkerDataTooltipComponent implements TooltipComponent {
    private final List<ItemStack> items;

    public ShulkerDataTooltipComponent(List<ItemStack> items) {
        this.items = items;
    }

    public List<ItemStack> getItems() {
        return items;
    }
}
