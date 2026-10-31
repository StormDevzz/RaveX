package ravex.gui.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

public class BookDataTooltipComponent implements TooltipComponent {
    private final ItemStack stack;

    public BookDataTooltipComponent(ItemStack stack) {
        this.stack = stack;
    }

    public ItemStack getStack() {
        return stack;
    }
}
