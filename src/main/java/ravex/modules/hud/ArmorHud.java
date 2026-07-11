package ravex.modules.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import ravex.modules.Module;
import ravex.modules.client.Hud;
import ravex.parameter.DependencyParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.utility.render.HudRenderer;
import ravex.utility.render.Render2DEngine;
import ravex.manager.ModuleManager;

public class ArmorHud extends Module {
    private static final EquipmentSlot[] SLOTS = {
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET
    };

    public final ModeParameter colorMode = new ModeParameter("ColorMode", "Dynamic", java.util.List.of("Dynamic", "Custom"));
    public final DependencyParameter<Integer, ColorParameter> customColor = new DependencyParameter<>(
        new ColorParameter("CustomColor", 0xFF44FF88), colorMode, "Custom"
    );

    private ArmorHud() {
        super("ArmorHud", 10, 260, 92, 29);
    }

    public static ArmorHud itz() {
        return ModuleManager.get(ArmorHud.class);
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(ArmorHud.class);
    }

    @Override
    public void render(GuiGraphics graphics, float partialTicks) {
        if (!ModuleManager.get(Hud.class).getEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int pw = 92;
        int ph = 29;
        int cellGap = 4;
        int cellSize = 18;

        setWidth(pw);
        setHeight(ph);

        int bx = getX(), by = getY();
        HudRenderer.drawBackground(graphics, bx, by, pw, ph);

        for (int i = 0; i < 4; i++) {
            EquipmentSlot slot = SLOTS[i];
            int cellX = bx + 4 + i * (cellSize + cellGap);
            int cellY = by + 4;

            Render2DEngine.drawRound(graphics, cellX, cellY, cellSize, cellSize, 3, 0x15FFFFFF);
            Render2DEngine.drawRoundBorder(graphics, cellX, cellY, cellSize, cellSize, 3, 1, 0x10FFFFFF);

            ItemStack stack = mc.player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, cellX + 1, cellY + 1);
                graphics.renderItemDecorations(mc.font, stack, cellX + 1, cellY + 1);

                if (stack.isDamageableItem()) {
                    float pct = (float) (stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage();
                    int barColor = colorMode.getValue().equals("Custom")
                        ? customColor.getValue()
                        : ravex.utility.render.ColorUtility.interpolate(0xFFFF3333, 0xFF33FF33, pct);

                    int barY = cellY + 20;
                    Render2DEngine.drawRound(graphics, cellX, barY, cellSize, 2, 1, 0x30000000);
                    Render2DEngine.drawRound(graphics, cellX, barY, Math.round(cellSize * pct), 2, 1, barColor);
                }
            }
        }
    }
}
