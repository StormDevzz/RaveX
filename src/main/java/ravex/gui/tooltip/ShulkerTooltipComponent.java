package ravex.gui.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;
import ravex.utility.render.Render2DUtility;

import java.util.List;

public class ShulkerTooltipComponent implements ClientTooltipComponent {
    private final List<ItemStack> items;
    private final int tintColor;

    public ShulkerTooltipComponent(List<ItemStack> items, int tintColor) {
        this.items = items;
        this.tintColor = tintColor;
    }

    @Override
    public int getHeight(Font font) {
        return 54;
    }

    @Override
    public int getWidth(Font font) {
        return 162;
    }

    @Override
    public void renderImage(Font font, int x, int y, int width, int height, GuiGraphics guiGraphics) {
        int panel = (tintColor & 0x00FFFFFF) | 0x90000000;
        int border = (tintColor & 0x00FFFFFF) | 0xC8000000;
        Render2DUtility.drawRound(guiGraphics, x - 3, y - 3, 168, 60, 5, panel);
        Render2DUtility.drawRoundBorder(guiGraphics, x - 3, y - 3, 168, 60, 5, 1, border);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = row * 9 + col;
                int slotX = x + col * 18;
                int slotY = y + row * 18;

                Render2DUtility.drawRound(guiGraphics, slotX, slotY, 16, 16, 2, 0x10FFFFFF);

                if (index < items.size()) {
                    ItemStack stack = items.get(index);
                    if (stack != null && !stack.isEmpty()) {
                        guiGraphics.renderItem(stack, slotX, slotY);
                        guiGraphics.renderItemDecorations(font, stack, slotX, slotY);
                    }
                }
            }
        }
    }
}
