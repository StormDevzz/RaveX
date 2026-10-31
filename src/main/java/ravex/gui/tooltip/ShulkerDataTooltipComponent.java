package ravex.gui.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import java.util.List;

public class ShulkerDataTooltipComponent implements TooltipComponent {
    private final List<ItemStack> items;
    private final int tintColor;

    public ShulkerDataTooltipComponent(List<ItemStack> items, int tintColor) {
        this.items = items;
        this.tintColor = tintColor;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public int getTintColor() {
        return tintColor;
    }
}
