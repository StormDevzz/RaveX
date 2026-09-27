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
import ravex.gui.hudeditor.HudEditorScreen;
import net.minecraft.world.item.ItemStack;

@HudModule("InvPreviewHud")
public class InvPreviewHud extends ravex.modules.Module {
    @Parameter(name = "AccentColor", color = true)
    public int accentColor = 0xFF1E88E5;
    @Parameter(name = "ShowLabel")
    public boolean showLabel = true;

    private static final Identifier ICON = TextureLoaderUtility.HUD_INVENTORY_WHITE;
    private static final int IS = HudRendererUtility.getIconSize();
    private static final int CELL = 18;
    private static final int GAP = 2;
    private static final int COLS = 9;
    private static final int PAD = 5;

    public InvPreviewHud() {
        super("InvPreviewHud", 10, 200, 188, 108);
        setX(10);
        setY(200);
        setWidth(188);
        setHeight(108);
    }

    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Modules.enabled(Hud.class)) return;
        var mc = MinecraftWrapper.getWrapper();
        boolean inEditor = mc.getScreen() instanceof HudEditorScreen;
        if (mc.getPlayer() == null && !inEditor) return;

        int accent = this.accentColor;
        int bx = getX();
        int by = getY();
        int headerH = this.showLabel ? 16 : 0;
        int innerW = COLS * CELL + (COLS - 1) * GAP;
        int pw = innerW + PAD * 2;
        int ph = PAD + headerH + 3 * (CELL + GAP) + 4 + CELL + PAD;

        setWidth(pw);
        setHeight(ph);

        Render2DUtility.drawRound(graphics, bx, by, pw, ph, 6, 0x850E0E14);
        Render2DUtility.drawRoundBorder(graphics, bx, by, pw, ph, 6, 1, ColorUtility.withAlpha(accent, 70));

        if (this.showLabel) {
            HudRendererUtility.drawLabel(graphics, "Inventory", bx + PAD + 1, by + 4, accent);
            HudRendererUtility.drawIcon(graphics, ICON, bx + pw - PAD - IS, by + 4, accent);
        }

        int startY = by + PAD + headerH;
        int selectedSlot = mc.getPlayer() != null ? ravex.utility.player.InventoryUtility.getSelectedSlot(mc.getPlayer()) : 0;

        for (int row = 0; row < 3; row++) {
            int slotY = startY + row * (CELL + GAP);
            for (int col = 0; col < COLS; col++) {
                int slotX = bx + PAD + col * (CELL + GAP);
                int slot = 9 + row * COLS + col;
                renderSlot(graphics, mc, slot, slotX, slotY, false, accent);
            }
        }

        int hotbarY = startY + 3 * (CELL + GAP) + 4;
        Render2DUtility.drawRect(graphics, bx + PAD, hotbarY - 3, innerW, 1, 0x1AFFFFFF);

        for (int col = 0; col < COLS; col++) {
            int slotX = bx + PAD + col * (CELL + GAP);
            boolean isSelected = selectedSlot == col;
            renderSlot(graphics, mc, col, slotX, hotbarY, isSelected, accent);
        }
    }

    private void renderSlot(GuiGraphics graphics, MinecraftWrapper mc, int inventorySlot, int x, int y, boolean highlight, int accent) {
        int bg = highlight ? ColorUtility.withAlpha(accent, 45) : 0x16FFFFFF;
        Render2DUtility.drawRound(graphics, x, y, CELL, CELL, 3, bg);

        int border = highlight ? ColorUtility.withAlpha(accent, 180) : 0x12FFFFFF;
        Render2DUtility.drawRoundBorder(graphics, x, y, CELL, CELL, 3, 1, border);

        if (mc.getPlayer() == null) return;
        ItemStack stack = ravex.utility.player.InventoryUtility.getItem(mc.getPlayer(), inventorySlot);
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, x + 1, y + 1);
            graphics.renderItemDecorations(mc.getFont(), stack, x + 1, y + 1);
        }
    }
}
