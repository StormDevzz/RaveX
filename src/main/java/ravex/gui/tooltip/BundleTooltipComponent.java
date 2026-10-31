package ravex.gui.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.apache.commons.lang3.math.Fraction;
import ravex.utility.render.Render2DUtility;
import java.util.ArrayList;
import java.util.List;

public class BundleTooltipComponent implements ClientTooltipComponent {
    private final ItemStack stack;
    private final List<ItemStack> items = new ArrayList<>();
    private final Fraction weight;

    public BundleTooltipComponent(ItemStack stack) {
        this.stack = stack;
        BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (contents != null) {
            for (ItemStack item : contents.items()) {
                this.items.add(item);
            }
            this.weight = contents.weight();
        } else {
            this.weight = Fraction.ZERO;
        }
    }

    private int getColumns() {
        return Math.max(4, Math.min(8, items.size()));
    }

    private int getRows() {
        return Math.max(1, (int) Math.ceil((double) Math.max(1, items.size()) / (double) getColumns()));
    }

    @Override
    public int getWidth(Font font) {
        return getColumns() * 18;
    }

    @Override
    public int getHeight(Font font) {
        return getRows() * 18 + 14;
    }

    @Override
    public void renderImage(Font font, int x, int y, int width, int height, GuiGraphics guiGraphics) {
        int w = getWidth(font);
        int h = getHeight(font);
        Render2DUtility.drawRound(guiGraphics, x - 3, y - 3, w + 6, h + 6, 4, 0xEE161720);
        Render2DUtility.drawRoundBorder(guiGraphics, x - 3, y - 3, w + 6, h + 6, 4, 1, 0xFF6C5340);

        int cols = getColumns();
        for (int i = 0; i < items.size(); i++) {
            int row = i / cols;
            int col = i % cols;
            int slotX = x + col * 18;
            int slotY = y + row * 18;
            Render2DUtility.drawRound(guiGraphics, slotX, slotY, 16, 16, 2, 0x14FFFFFF);
            ItemStack item = items.get(i);
            if (!item.isEmpty()) {
                guiGraphics.renderItem(item, slotX, slotY);
                guiGraphics.renderItemDecorations(font, item, slotX, slotY);
            }
        }

        int barY = y + getRows() * 18 + 4;
        float progress = Math.min(1.0f, (float) weight.doubleValue());
        Render2DUtility.drawRound(guiGraphics, x, barY, w, 4, 2, 0x33FFFFFF);
        int fillW = Math.round(w * progress);
        if (fillW > 0) {
            int barColor = progress >= 1.0f ? 0xFFE05050 : 0xFF55AAFF;
            Render2DUtility.drawRound(guiGraphics, x, barY, fillW, 4, 2, barColor);
        }
    }
}
