package ravex.modules.player;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.player.ArmorUtility;
import ravex.utility.player.InventoryUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import java.util.List;
public class AutoArmor extends Module {
    public static final AutoArmor INSTANCE = new AutoArmor();
    public final ModeParameter mode = new ModeParameter("Mode", "Normal",
            List.of("Normal", "Legit"));
    public final NumberParameter delay = new NumberParameter("Delay(ms)", 150.0, 0.0, 1000.0, 10.0);
    public final BooleanParameter onlyBetter = new BooleanParameter("OnlyBetter", true);
    private long lastEquipTime = 0;

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;
        if (mc.screen != null && !(mc.screen instanceof InventoryScreen)) return;
        if (System.currentTimeMillis() - lastEquipTime < delay.getValue()) return;
        for (int armorIndex = 0; armorIndex < 4; armorIndex++) {
            EquipmentSlot equipSlot = ArmorUtility.getEquipmentSlotForIndex(armorIndex);
            ItemStack currentArmor = p.getItemBySlot(equipSlot);
            int bestSlot = ArmorUtility.findBestArmorSlot(p, armorIndex);
            if (bestSlot == -1) continue;
            ItemStack bestStack = p.getInventory().getItem(bestSlot);
            if (!ArmorUtility.isArmorItem(bestStack) || !ArmorUtility.slotMatches(bestStack, equipSlot)) continue;
            if (onlyBetter.getValue() && !currentArmor.isEmpty()
                && !ArmorUtility.isBetterArmor(bestStack, currentArmor, equipSlot)) continue;
            if ("Legit".equals(mode.getValue())) {
                int hotbarSlot = InventoryUtility.findEmptyHotbarSlot(p);
                if (hotbarSlot == -1) hotbarSlot = getLowestPriorityHotbarSlot(p);
                if (hotbarSlot == -1 || hotbarSlot > 8) continue;
                InventoryUtility.quickMoveStack(mc, p, bestSlot);
                InventoryUtility.quickMoveStack(mc, p, hotbarSlot);
                int prevSlot = p.getInventory().getSelectedSlot();
                p.getInventory().setSelectedSlot(hotbarSlot);
                mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
                if (prevSlot != hotbarSlot) p.getInventory().setSelectedSlot(prevSlot);
            } else {
                InventoryUtility.quickMoveStack(mc, p, bestSlot);
            }
            lastEquipTime = System.currentTimeMillis();
            break;
        }
    }
    private int getLowestPriorityHotbarSlot(LocalPlayer p) {
        int worstSlot = 0;
        double worstScore = Double.MAX_VALUE;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = p.getInventory().getItem(i);
            double score = 0;
            if (!stack.isEmpty()) {
                if (ArmorUtility.isArmorItem(stack)) {
                    EquipmentSlot slot = stack.get(DataComponents.EQUIPPABLE).slot();
                    score = ArmorUtility.getArmorScore(stack, slot);
                } else {
                    score = 1;
                }
            }
            if (score < worstScore) { worstScore = score; worstSlot = i; }
        }
        return worstSlot;
    }
}
