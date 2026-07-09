package ravex.modules.hud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import ravex.gui.clickgui.ColorUtility;
import ravex.modules.Module;
import ravex.modules.client.Hud;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
<<<<<<< HEAD
import ravex.utility.player.InventoryUtility;
import ravex.utility.render.HudRenderer;
import ravex.utility.render.TextureLoader;
import ravex.manager.ModuleManager;
public class InvPreviewHud extends Module {
    private static final Identifier ICON = TextureLoader.HUD_INVENTORY_WHITE;
    private static final int IS = HudRenderer.getIconSize();
=======
public class InvPreviewHud extends Module {
    public static final InvPreviewHud INSTANCE = new InvPreviewHud();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    private static final int CELL = 16;
    private static final int PAD  = 2;
    private static final int COLS = 9;
    private InvPreviewHud() {
        super("InvPreview", 10, 280, COLS * (CELL + PAD) + PAD, 4 * (CELL + PAD) + PAD + 12);
        addParameter(new ColorParameter("AccentColor", 0xFF1E88E5));
        addParameter(new BooleanParameter("ShowLabel", true));
    }
    private int getAccent() {
        for (var p : getParameters()) {
            if (p instanceof ColorParameter cp) return cp.getValue();
        }
        return ColorUtility.getActiveColor();
    }
    private boolean showLabel() {
        for (var p : getParameters()) {
            if (p instanceof BooleanParameter bp && p.getName().equals("ShowLabel")) return bp.getValue();
        }
        return true;
    }
    @Override
    public void render(GuiGraphics graphics, float partialTicks) {
        if (!ModuleManager.get(Hud.class).getEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        int accent = getAccent();
        int bx = getX();
        int by = getY();
        int w  = getWidth();
        int h  = getHeight();
<<<<<<< HEAD
        HudRenderer.drawBackground(graphics, bx, by, w, h);
        if (showLabel()) {
            HudRenderer.drawLabel(graphics, "Inventory", bx + 4, by, accent);
            HudRenderer.drawIcon(graphics, ICON, bx + w - 4 - IS, by + 4, accent);
        }
        int startY = by + (showLabel() ? 14 : 5);
=======
        ravex.utility.render.HudRenderer.drawPanel(graphics, bx, by, w, h, accent);
        if (showLabel()) {
            ravex.utility.render.HudRenderer.drawLabel(graphics, "Inventory", bx, by, accent);
        }
        int startY = by + (showLabel() ? 12 : 3);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < COLS; col++) {
                int slot = 9 + row * COLS + col;
                renderSlot(graphics, mc, slot, bx + PAD + col * (CELL + PAD), startY + PAD + row * (CELL + PAD), false, accent);
            }
        }
        int hotbarY = startY + PAD + 3 * (CELL + PAD);
        graphics.fill(bx + 1, hotbarY - 1, bx + w - 1, hotbarY + CELL + PAD + 1, 0x22FFFFFF);
        for (int col = 0; col < COLS; col++) {
<<<<<<< HEAD
            boolean isSelected = InventoryUtility.getSelectedSlot(mc.player) == col;
=======
            boolean isSelected = mc.player.getInventory().getSelectedSlot() == col;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            renderSlot(graphics, mc, col, bx + PAD + col * (CELL + PAD), hotbarY, isSelected, accent);
        }
    }
    private void renderSlot(GuiGraphics graphics, Minecraft mc, int inventorySlot, int x, int y, boolean highlight, int accent) {
        int bg = highlight ? ColorUtility.withAlpha(accent, 40) : 0x22FFFFFF;
        graphics.fill(x, y, x + CELL, y + CELL, bg);
        if (highlight) {
            graphics.fill(x, y, x + CELL, y + 1, accent);
        }
<<<<<<< HEAD
        var stack = InventoryUtility.getItem(mc.player, inventorySlot);
=======
        ItemStack stack = mc.player.getInventory().getItem(inventorySlot);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, x, y);
            if (stack.getCount() > 1) {
                String countStr = stack.getCount() >= 64 ? "64" : String.valueOf(stack.getCount());
                graphics.renderItemDecorations(mc.font, stack, x, y, countStr);
            }
        }
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(InvPreviewHud.class);
    }

    public static InvPreviewHud itz() {
        return ModuleManager.get(InvPreviewHud.class);
    }
}
