package ravex.modules.hud;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import ravex.gui.clickgui.ColorUtility;

import ravex.modules.client.Hud;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.utility.player.InventoryUtility;
import ravex.utility.render.HudRendererUtility;
import ravex.utility.render.TextureLoaderUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;

@Module(name = "InvPreviewHud", category = "HUD")
public class InvPreviewHud extends ravex.modules.Module {
    @Parameter(name = "AccentColor", color = true)
    public int accentColor = 0xFF1E88E5;
    @Parameter(name = "ShowLabel")
    public boolean showLabel = true;

    public int x;
    public int y;
    public int width;
    public int height;
private static final Identifier ICON = TextureLoaderUtility.HUD_INVENTORY_WHITE;
    private static final int IS = HudRendererUtility.getIconSize();
    private static final int CELL = 16;
    private static final int PAD  = 2;
    private static final int COLS = 9;

    private int getAccent() {
        for (var p : getParameters()) {
            if (p instanceof ColorParameter cp) return cp.getValue();
        }
        return ColorUtility.getActiveColor();
    }
    private boolean showLabel() {
        for (var p : getParameters()) {
            if (p instanceof BooleanParameter bp && bp.getName().equals("ShowLabel")) return bp.getValue();
        }
        return true;
    }
    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Modules.enabled(Hud.class)) return;
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null) return;
        int accent = getAccent();
        int bx = x;
        int by = y;
        int w  = width;
        int h  = height;
        HudRendererUtility.drawBackground(graphics, bx, by, w, h);
        if (showLabel()) {
            HudRendererUtility.drawLabel(graphics, "Inventory", bx + 4, by, accent);
            HudRendererUtility.drawIcon(graphics, ICON, bx + w - 4 - IS, by + 4, accent);
        }
        int startY = by + (showLabel() ? 14 : 5);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < COLS; col++) {
                int slot = 9 + row * COLS + col;
                renderSlot(graphics, mc, slot, bx + PAD + col * (CELL + PAD), startY + PAD + row * (CELL + PAD), false, accent);
            }
        }
        int hotbarY = startY + PAD + 3 * (CELL + PAD);
        graphics.fill(bx + 1, hotbarY - 1, bx + w - 1, hotbarY + CELL + PAD + 1, 0x22FFFFFF);
        for (int col = 0; col < COLS; col++) {
            boolean isSelected = InventoryUtility.getSelectedSlot(mc.getPlayer()) == col;
            renderSlot(graphics, mc, col, bx + PAD + col * (CELL + PAD), hotbarY, isSelected, accent);
        }
    }
    private void renderSlot(GuiGraphics graphics, MinecraftWrapper mc, int inventorySlot, int x, int y, boolean highlight, int accent) {
        int bg = highlight ? ColorUtility.withAlpha(accent, 40) : 0x22FFFFFF;
        graphics.fill(x, y, x + CELL, y + CELL, bg);
        if (highlight) {
            graphics.fill(x, y, x + CELL, y + 1, accent);
        }
        var stack = InventoryUtility.getItem(mc.getPlayer(), inventorySlot);
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, x, y);
            if (stack.getCount() > 1) {
                String countStr = stack.getCount() >= 64 ? "64" : String.valueOf(stack.getCount());
                graphics.renderItemDecorations(mc.getFont(), stack, x, y, countStr);
            }
        }
    }






    

    @Override
    public int getX() { return x; }
    @Override
    public void setX(int x) { this.x = x; }
    @Override
    public int getY() { return y; }
    @Override
    public void setY(int y) { this.y = y; }
    @Override
    public int getWidth() { return width; }
    @Override
    public void setWidth(int w) { this.width = w; }
    @Override
    public int getHeight() { return height; }
    @Override
    public void setHeight(int h) { this.height = h; }

    public boolean isHud() {
        return hud;
    }
}