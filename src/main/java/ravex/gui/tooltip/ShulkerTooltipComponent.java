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

        int bgColor = 0xAA0B0B12;
        int accentColor = ColorUtility.getActiveColor();
        int borderColor = ColorUtility.withAlpha(accentColor, 100);

        Render2DEngine.drawRound(guiGraphics, x - 4, y - 4, w + 8, h + 8, 4, bgColor);
        Render2DEngine.drawRoundBorder(guiGraphics, x - 4, y - 4, w + 8, h + 8, 4, 1, borderColor);


        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = row * 9 + col;
                int slotX = x + col * 18;
                int slotY = y + row * 18;

                Render2DEngine.drawRound(guiGraphics, slotX, slotY, 16, 16, 2, 0x10FFFFFF);

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
