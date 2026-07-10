package ravex.gui.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;
import ravex.gui.clickgui.ColorUtility;
import ravex.utility.render.Render2DEngine;

import java.util.List;

public class ShulkerTooltipComponent implements ClientTooltipComponent {
    private final List<ItemStack> items;

    public ShulkerTooltipComponent(List<ItemStack> items) {
        this.items = items;
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
        int w = getWidth(font);
        int h = getHeight(font);


        guiGraphics.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xF00C0C14);


        int accentColor = ColorUtility.getActiveColor();
        Render2DEngine.drawBorder(guiGraphics, x - 2, y - 2, w + 4, h + 4, 1, accentColor);


        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = row * 9 + col;
                int slotX = x + col * 18;
                int slotY = y + row * 18;


                guiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0x15FFFFFF);

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
