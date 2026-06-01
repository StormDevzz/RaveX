package ravex.modules.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import ravex.gui.clickgui.ColorUtility;
import ravex.modules.HudModule;
import ravex.modules.render.Hud;

/**
 * InvPreviewHud – shows the player's inventory as a compact icon grid.
 * Hotbar (9 items) on the bottom row, inventory above.
 */
public class InvPreviewHud extends HudModule {
    public static final InvPreviewHud INSTANCE = new InvPreviewHud();

    // Cell size + padding
    private static final int CELL = 16;
    private static final int PAD  = 2;
    private static final int COLS = 9;

    private InvPreviewHud() {
        // 9 cols × (CELL+PAD) wide, 4 rows tall (+header 10px)
        super("InvPreview", 10, 280, COLS * (CELL + PAD) + PAD, 4 * (CELL + PAD) + PAD + 12);
    }

    @Override
    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Hud.INSTANCE.getEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int activeColor = ColorUtility.getActiveColor();
        int bx = getX();
        int by = getY();
        int w  = getWidth();
        int h  = getHeight();

        // Background
        graphics.fill(bx, by, bx + w, by + h, 0xBB060610);
        graphics.fill(bx, by, bx + w, by + 1, ColorUtility.withAlpha(activeColor, 120));

        // Label
        ravex.utility.render.FontRenderUtility.drawString(graphics, "Inventory", bx + 3, by + 3, ColorUtility.withAlpha(activeColor, 200), false);

        int startY = by + 12;

        // Rows 0-2: main inventory (items 9-35)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < COLS; col++) {
                int slot = 9 + row * COLS + col;
                renderSlot(graphics, mc, slot, bx + PAD + col * (CELL + PAD), startY + PAD + row * (CELL + PAD), false, activeColor);
            }
        }

        // Row 3: hotbar (items 0-8), highlighted
        int hotbarY = startY + PAD + 3 * (CELL + PAD);
        // Hotbar background strip
        graphics.fill(bx + 1, hotbarY - 1, bx + w - 1, hotbarY + CELL + PAD + 1, 0x22FFFFFF);
        for (int col = 0; col < COLS; col++) {
            boolean isSelected = mc.player.getInventory().getSelectedSlot() == col;
            renderSlot(graphics, mc, col, bx + PAD + col * (CELL + PAD), hotbarY, isSelected, activeColor);
        }
    }

    private void renderSlot(GuiGraphics graphics, Minecraft mc, int inventorySlot, int x, int y, boolean highlight, int activeColor) {
        // Slot background
        int bg = highlight ? ColorUtility.withAlpha(activeColor, 40) : 0x22FFFFFF;
        graphics.fill(x, y, x + CELL, y + CELL, bg);
        if (highlight) {
            graphics.fill(x, y, x + CELL, y + 1, activeColor);
        }

        ItemStack stack = mc.player.getInventory().getItem(inventorySlot);
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, x, y);
            // Count
            if (stack.getCount() > 1) {
                String countStr = stack.getCount() >= 64 ? "64" : String.valueOf(stack.getCount());
                graphics.renderItemDecorations(mc.font, stack, x, y, countStr);
            }
        }
    }
}
