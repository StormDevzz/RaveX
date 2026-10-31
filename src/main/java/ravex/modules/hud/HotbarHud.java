package ravex.modules.hud;

import ravex.modules.annotations.HudModule;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import ravex.modules.client.Hud;
import ravex.utility.player.InventoryUtility;
import ravex.utility.render.ColorUtility;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DUtility;
import ravex.utility.render.HudRendererUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;

@HudModule("HotbarHud")
public class HotbarHud extends ravex.modules.Module {
    private static final int SLOT = 18;
    private static final int GAP = 1;
    private static final int PAD = 3;
    private static final int PANEL_W = PAD * 2 + 9 * SLOT + 8 * GAP;
    private static final int PANEL_H = PAD * 2 + SLOT;
    private static final float SMALL_SCALE = 0.78f;
    private static final int OFFSET_X = 10;
    private static final int OFFSET_Y = 3;

    private float animSlotX = -1f;
    private float slotPop = 0f;
    private int lastSelected = -1;
    private long lastFrameNs = 0L;

    public HotbarHud() {
        super("HotbarHud", 100, 200, PANEL_W, PANEL_H);
    }

    public static boolean hidesVanillaHotbar() {
        return Modules.enabled(HotbarHud.class) && Modules.enabled(Hud.class);
    }

    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Modules.enabled(Hud.class)) return;
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null) return;

        setWidth(PANEL_W);
        setHeight(PANEL_H);
        if (!isHudPositionCustomized()) {
            var window = mc.getWindow();
            int gw = window != null ? window.getGuiScaledWidth() : 320;
            int gh = window != null ? window.getGuiScaledHeight() : 240;
            setX((gw - PANEL_W) / 2);
            setY(gh - PANEL_H);
        }

        long now = System.nanoTime();
        float dt = lastFrameNs == 0L ? 0.016f : Math.min(0.05f, (now - lastFrameNs) / 1_000_000_000f);
        lastFrameNs = now;

        int bx = getX() + OFFSET_X, by = getY() + OFFSET_Y;
        int ac = ColorUtility.getActiveColor();
        int sel = InventoryUtility.getSelectedSlot(mc.getPlayer());
        int slotY = by + PAD;

        float targetX = bx + PAD + sel * (SLOT + GAP);
        if (animSlotX < 0f) animSlotX = targetX;
        animSlotX += (targetX - animSlotX) * Math.min(1f, dt * 18f);
        if (Math.abs(targetX - animSlotX) < 0.4f) animSlotX = targetX;
        if (sel != lastSelected) {
            lastSelected = sel;
            slotPop = 1f;
        }
        slotPop = Math.max(0f, slotPop - dt * 4.5f);

        HudRendererUtility.drawBackground(graphics, bx, by, PANEL_W, PANEL_H);

        float pop = 1f + 0.12f * slotPop;
        int hlSize = Math.round(SLOT * pop);
        int hlX = Math.round(animSlotX + (SLOT - hlSize) / 2f);
        int hlY = Math.round(slotY + (SLOT - hlSize) / 2f);
        Render2DUtility.drawRound(graphics, hlX, hlY, hlSize, hlSize, 4, ColorUtility.withAlpha(ac, 70));
        Render2DUtility.drawRound(graphics, Math.round(animSlotX), by + PANEL_H - 2, SLOT, 2, 1, ac);

        int slotX = bx + PAD;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = InventoryUtility.getItem(mc.getPlayer(), i);
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, slotX + 1, slotY + 1);
                if (stack.getCount() > 1) {
                    String count = String.valueOf(stack.getCount());
                    drawSmall(graphics, count, slotX + SLOT - 1 - smallWidth(count), slotY + SLOT - 11, 0xFFFFFFFF);
                }
                if (stack.isDamageableItem()) {
                    durability(graphics, stack, slotX + 1, slotY + SLOT - 3, SLOT - 2);
                }
            }
            slotX += SLOT + GAP;
        }
    }

    private void durability(GuiGraphics graphics, ItemStack stack, int x, int y, int width) {
        int max = Math.max(1, stack.getMaxDamage());
        float pct = (float) (max - stack.getDamageValue()) / max;
        pct = Math.max(0f, Math.min(1f, pct));
        Render2DUtility.drawRound(graphics, x, y, width, 2, 1, 0x50000000);
        int fill = Math.round(width * pct);
        if (fill > 0) {
            Render2DUtility.drawRound(graphics, x, y, fill, 2, 1,
                ColorUtility.interpolate(0xFFFF3333, 0xFF33FF33, pct));
        }
    }

    private static void drawSmall(GuiGraphics graphics, String text, int x, int y, int color) {
        double base = ravex.modules.client.Fonts.getActiveFontSize();
        ravex.utility.render.FontRenderUtility.drawScaled(graphics,
            ravex.utility.render.FontRenderUtility.getCurrentFontType(), text,
            x, y, (float) (base * SMALL_SCALE), color, true);
    }

    private static int smallWidth(String text) {
        return (int) (ravex.utility.render.FontRenderUtility.getStringWidth(text) * SMALL_SCALE);
    }
}
