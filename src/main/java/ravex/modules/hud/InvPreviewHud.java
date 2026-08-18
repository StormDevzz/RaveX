package ravex.modules.hud;
import ravex.modules.annotations.HudModule;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import ravex.utility.render.ColorUtility;

import ravex.modules.client.Hud;
import ravex.utility.render.Render2DUtility;
import ravex.utility.render.HudRendererUtility;
import ravex.utility.render.TextureLoaderUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;

@HudModule("InvPreviewHud")
public class InvPreviewHud extends ravex.modules.Module {
    @Parameter(name = "AccentColor", color = true)
    public int accentColor = 0xFF1E88E5;
    @Parameter(name = "ShowLabel")
    public boolean showLabel = true;

private static final Identifier ICON = TextureLoaderUtility.HUD_INVENTORY_WHITE;
    private static final int IS = HudRendererUtility.getIconSize();
    private static final int CELL = 16;
    private static final int PAD  = 2;
    private static final int COLS = 9;

    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Modules.enabled(Hud.class)) return;
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null) return;
        int accent = this.accentColor;
        int bx = getX();
        int by = getY();
        int w  = getWidth();
        int h  = getHeight();
        HudRendererUtility.drawBackground(graphics, bx, by, w, h);
        if (this.showLabel) {
            HudRendererUtility.drawLabel(graphics, "Inventory", bx + 4, by, accent);
            HudRendererUtility.drawIcon(graphics, ICON, bx + w - 4 - IS, by + 4, accent);
        }
        int startY = by + (this.showLabel ? 14 : 5);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < COLS; col++) {
                int slot = 9 + row * COLS + col;
                renderSlot(graphics, mc, slot, bx + PAD + col * (CELL + PAD), startY + PAD + row * (CELL + PAD), false, accent);
            }
        }
        int hotbarY = startY + PAD + 3 * (CELL + PAD);
        Render2DUtility.drawRect(graphics, bx + 1, hotbarY - 1, w - 2, CELL + PAD + 2, 0x22FFFFFF);
        for (int col = 0; col < COLS; col++) {
            boolean isSelected = ravex.utility.player.InventoryUtility.getSelectedSlot(mc.getPlayer()) == col;
            renderSlot(graphics, mc, col, bx + PAD + col * (CELL + PAD), hotbarY, isSelected, accent);
        }
    }
    private void renderSlot(GuiGraphics graphics, MinecraftWrapper mc, int inventorySlot, int x, int y, boolean highlight, int accent) {
        int bg = highlight ? ColorUtility.withAlpha(accent, 40) : 0x22FFFFFF;
        Render2DUtility.drawRect(graphics, x, y, CELL, CELL, bg);
        if (highlight) {
            Render2DUtility.drawRect(graphics, x, y, CELL, 1, accent);
        }
        var stack = ravex.utility.player.InventoryUtility.getItem(mc.getPlayer(), inventorySlot);
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, x, y);
            if (stack.getCount() > 1) {
                String countStr = stack.getCount() >= 64 ? "64" : String.valueOf(stack.getCount());
                graphics.renderItemDecorations(mc.getFont(), stack, x, y, countStr);
            }
        }
    }
}
