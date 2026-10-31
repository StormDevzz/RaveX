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
import ravex.utility.player.InventoryUtility;
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
    @Parameter(name = "ShowCounter")
    public boolean showCounter = true;

    private static final Identifier ICON = TextureLoaderUtility.HUD_INVENTORY_WHITE;
    private static final int IS = HudRendererUtility.getIconSize();
    private static final int CELL = 18;
    private static final int GAP = 1;
    private static final int COLS = 9;
    private static final int ROWS = 3;
    private static final int PAD = 5;

    private float previewAlpha = 1f;
    private long previewLastNs = 0L;

    public InvPreviewHud() {
        super("InvPreviewHud", 10, 200, 188, 108);
        setX(10);
        setY(200);
    }

    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Modules.enabled(Hud.class)) return;
        var mc = MinecraftWrapper.getWrapper();
        boolean inEditor = mc.getScreen() instanceof HudEditorScreen;
        if (mc.getPlayer() == null && !inEditor) return;

        int accent = this.accentColor;
        int bx = getX();
        int by = getY();
        int headerH = this.showLabel ? 15 : 2;
        int innerW = COLS * CELL + (COLS - 1) * GAP;
        int rowsH = ROWS * CELL + (ROWS - 1) * GAP;
        int pw = innerW + PAD * 2;
        int ph = PAD + headerH + rowsH + 4 + CELL + PAD + 4;

        setWidth(pw);
        setHeight(ph);

        HudRendererUtility.drawBackground(graphics, bx, by, pw, ph, previewAlpha);

        int used = countUsed(mc);
        long now = System.nanoTime();
        float dt = previewLastNs == 0L ? 0.016f : Math.min(0.05f, (now - previewLastNs) / 1_000_000_000f);
        previewLastNs = now;
        boolean shouldShow = used > 0 || inEditor;
        float target = shouldShow ? 1f : 0f;
        previewAlpha += (target - previewAlpha) * Math.min(1f, dt * 6f);
        if (previewAlpha <= 0.001f) return;

        if (this.showLabel) {
            HudRendererUtility.drawIcon(graphics, ICON, bx + PAD, by + 5,
                ColorUtility.multiplyAlpha(ColorUtility.withAlpha(accent, 190), previewAlpha));
            HudRendererUtility.drawText(graphics, "Inventory", bx + PAD + IS + 5, by + 6,
                ColorUtility.multiplyAlpha(0xFFC8C8D8, previewAlpha), false);
            if (this.showCounter) {
                String counter = used + "/36";
                int counterW = HudRendererUtility.textWidth(counter);
                HudRendererUtility.drawText(graphics, counter, bx + pw - PAD - counterW, by + 6,
                    ColorUtility.multiplyAlpha(ColorUtility.withAlpha(accent, 170), previewAlpha), false);
            }
        }

        int startY = by + PAD + headerH;
        Render2DUtility.drawRect(graphics, bx + PAD, startY - 4, innerW, 1,
            ColorUtility.multiplyAlpha(0x14FFFFFF, previewAlpha));

        int selectedSlot = mc.getPlayer() != null ? InventoryUtility.getSelectedSlot(mc.getPlayer()) : 0;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int slot = 9 + row * COLS + col;
                int x = bx + PAD + col * (CELL + GAP);
                int y = startY + row * (CELL + GAP);
                renderSlot(graphics, mc, slot, x, y);
            }
        }

        int hotbarY = startY + rowsH + 4;
        for (int col = 0; col < COLS; col++) {
            int x = bx + PAD + col * (CELL + GAP);
            boolean selected = selectedSlot == col;
            if (selected) {
                Render2DUtility.drawRound(graphics, x - 1, hotbarY - 1, CELL + 2, CELL + 2, 4,
                    ColorUtility.multiplyAlpha(ColorUtility.withAlpha(accent, 45), previewAlpha));
            }
            renderSlot(graphics, mc, col, x, hotbarY);
            if (selected) {
                Render2DUtility.drawRound(graphics, x, hotbarY + CELL + 1, CELL, 2, 1,
                    ColorUtility.multiplyAlpha(ColorUtility.withAlpha(accent, 215), previewAlpha));
            }
        }
    }

    private int countUsed(MinecraftWrapper mc) {
        if (mc.getPlayer() == null) return 0;
        int used = 0;
        for (int i = 0; i < 36; i++) {
            if (!InventoryUtility.getItem(mc.getPlayer(), i).isEmpty()) used++;
        }
        return used;
    }

        private void renderSlot(GuiGraphics graphics, MinecraftWrapper mc, int inventorySlot, int x, int y) {
            if (mc.getPlayer() == null) return;
            ItemStack stack = InventoryUtility.getItem(mc.getPlayer(), inventorySlot);
            if (stack.isEmpty()) return;
            graphics.renderItem(stack, x + 1, y + 1);
            graphics.renderItemDecorations(mc.getFont(), stack, x + 1, y + 1);
        }
}
