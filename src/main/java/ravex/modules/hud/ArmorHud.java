package ravex.modules.hud;
import ravex.modules.annotations.HudModule;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import ravex.modules.client.Hud;
import ravex.utility.render.ColorUtility;
import ravex.utility.render.HudRendererUtility;
import ravex.utility.render.Render2DUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;

@HudModule("ArmorHud")
public class ArmorHud extends ravex.modules.Module {
    private static final EquipmentSlot[] SLOTS = {
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET
    };

    private static final int CELL = 20;
    private static final int GAP = 2;
    private static final int PAD = 3;

    @Parameter(name = "ColorMode", modes = {"Dynamic", "Custom"})
    public String colorMode = "Dynamic";
    @Parameter(name = "CustomColor", color = true, visible = "colorMode=Custom")
    public int customColor = 0xFF44FF88;
    @Parameter(name = "HideEmpty")
    public boolean hideEmpty = true;
    @Parameter(name = "ShowBar")
    public boolean showBar = true;

    private float armorAlpha = 0f;
    private long armorLastNs = 0L;

    private ArmorHud() {
        super("ArmorHud", 30, 30, 80, 20);
        setX(10); setY(260); setWidth(92); setHeight(26);
    }

    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Modules.enabled(Hud.class)) return;
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null) return;

        int worn = 0;
        for (EquipmentSlot slot : SLOTS) {
            if (!mc.getPlayer().getItemBySlot(slot).isEmpty()) worn++;
        }
        boolean hasArmor = worn > 0;

        long now = System.nanoTime();
        float dt = armorLastNs == 0L ? 0.016f : Math.min(0.05f, (now - armorLastNs) / 1_000_000_000f);
        armorLastNs = now;
        float targetAlpha = hasArmor ? 1f : 0f;
        armorAlpha += (targetAlpha - armorAlpha) * Math.min(1f, dt * 8f);

        if (armorAlpha <= 0.005f && !hasArmor) {
            setWidth(0);
            setHeight(0);
            return;
        }

        int cells = hideEmpty ? worn : SLOTS.length;
        int pw = PAD * 2 + cells * CELL + Math.max(0, cells - 1) * GAP;
        int ph = PAD * 2 + CELL;
        setWidth(pw);
        setHeight(ph);

        int bx = getX(), by = getY();
        HudRendererUtility.drawBackground(graphics, bx, by, pw, ph);

        int cellX = bx + PAD;
        int cellY = by + PAD;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = mc.getPlayer().getItemBySlot(slot);
            if (stack.isEmpty()) {
                if (hideEmpty) continue;
                Render2DUtility.drawRound(graphics, cellX, cellY, CELL, CELL, 4, 0x0AFFFFFF);
                cellX += CELL + GAP;
                continue;
            }

            int cellAlpha = (int) (0xFF * armorAlpha);
            Render2DUtility.drawRound(graphics, cellX, cellY, CELL, CELL, 4,
                ColorUtility.multiplyAlpha(0x14FFFFFF, cellAlpha / 255f));
            graphics.renderItem(stack, cellX + 2, cellY + 1);

            if (showBar && stack.isDamageableItem()) {
                int max = Math.max(1, stack.getMaxDamage());
                float pct = (float) (max - stack.getDamageValue()) / max;
                pct = Math.max(0f, Math.min(1f, pct));
                int barColor = "Custom".equals(colorMode)
                    ? customColor
                    : ColorUtility.interpolate(0xFFFF3333, 0xFF33FF33, pct);
                barColor = ColorUtility.multiplyAlpha(barColor, armorAlpha);
                int barX = cellX + 2;
                int barY = cellY + CELL - 3;
                int barW = CELL - 4;
                Render2DUtility.drawRound(graphics, barX, barY, barW, 2, 1,
                    ColorUtility.multiplyAlpha(0x50000000, armorAlpha));
                int fillW = Math.round(barW * pct);
                if (fillW > 0) {
                    Render2DUtility.drawRound(graphics, barX, barY, fillW, 2, 1, barColor);
                }
            }

            cellX += CELL + GAP;
        }
    }
}
