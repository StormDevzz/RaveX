package ravex.modules.player;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.DependencyParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.player.ArmorUtility;
import ravex.utility.player.InventoryUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
<<<<<<< HEAD
=======
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.equipment.Equippable;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import java.util.List;
public class AutoArmor extends Module {
<<<<<<< HEAD
    public final ModeParameter mode = new ModeParameter("Mode", "Normal",
            List.of("Normal", "Legit", "Custom"));
    public final NumberParameter delay = new NumberParameter("Delay", 150.0, 0.0, 1000.0, 10.0);
    public final BooleanParameter onlyBetter = new BooleanParameter("OnlyBetter", true);
    public final DependencyParameter<Double, NumberParameter> customDelay =
            new DependencyParameter<>(new NumberParameter("CustomDelay", 50.0, 0.0, 500.0, 10.0), mode, "Custom");
    public final DependencyParameter<Boolean, BooleanParameter> openInventory =
            new DependencyParameter<>(new BooleanParameter("OpenInventory", true), mode, "Custom");
    public final DependencyParameter<Boolean, BooleanParameter> ignoreEnchants =
            new DependencyParameter<>(new BooleanParameter("IgnoreEnchants", false), mode, "Custom");
=======
    public static final AutoArmor INSTANCE = new AutoArmor();
    public final ModeParameter mode = new ModeParameter("Mode", "Normal",
            List.of("Normal", "Legit"));
    public final NumberParameter delay = new NumberParameter("Delay(ms)", 150.0, 0.0, 1000.0, 10.0);
    public final BooleanParameter onlyBetter = new BooleanParameter("OnlyBetter", true);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    private long lastEquipTime = 0;

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;
<<<<<<< HEAD
        String currentMode = mode.getValue();
        if ("Custom".equals(currentMode)) {
            tickCustom(mc, p);
        } else {
            tickNormal(mc, p, currentMode);
        }
    }

    private void tickNormal(Minecraft mc, LocalPlayer p, String currentMode) {
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (mc.screen != null && !(mc.screen instanceof InventoryScreen)) return;
        if (System.currentTimeMillis() - lastEquipTime < delay.getValue()) return;
        for (int armorIndex = 0; armorIndex < 4; armorIndex++) {
            EquipmentSlot equipSlot = ArmorUtility.getEquipmentSlotForIndex(armorIndex);
<<<<<<< HEAD
            var currentArmor = p.getItemBySlot(equipSlot);
            int bestSlot = ArmorUtility.findBestArmorSlot(p, armorIndex);
            if (bestSlot == -1) continue;
            var bestStack = InventoryUtility.getItem(p, bestSlot);
            if (!ArmorUtility.isArmorItem(bestStack) || !ArmorUtility.slotMatches(bestStack, equipSlot)) continue;
            if (onlyBetter.getValue() && !currentArmor.isEmpty()
                && !ArmorUtility.isBetterArmor(bestStack, currentArmor, equipSlot)) continue;
            if ("Legit".equals(currentMode)) {
=======
            ItemStack currentArmor = p.getItemBySlot(equipSlot);
            int bestSlot = ArmorUtility.findBestArmorSlot(p, armorIndex);
            if (bestSlot == -1) continue;
            ItemStack bestStack = p.getInventory().getItem(bestSlot);
            if (!ArmorUtility.isArmorItem(bestStack) || !ArmorUtility.slotMatches(bestStack, equipSlot)) continue;
            if (onlyBetter.getValue() && !currentArmor.isEmpty()
                && !ArmorUtility.isBetterArmor(bestStack, currentArmor, equipSlot)) continue;
            if ("Legit".equals(mode.getValue())) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                int hotbarSlot = InventoryUtility.findEmptyHotbarSlot(p);
                if (hotbarSlot == -1) hotbarSlot = getLowestPriorityHotbarSlot(p);
                if (hotbarSlot == -1 || hotbarSlot > 8) continue;
                InventoryUtility.quickMoveStack(mc, p, bestSlot);
                InventoryUtility.quickMoveStack(mc, p, hotbarSlot);
<<<<<<< HEAD
                int prevSlot = InventoryUtility.getSelectedSlot(p);
                InventoryUtility.selectSlot(p, hotbarSlot);
                mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
                if (prevSlot != hotbarSlot) InventoryUtility.selectSlot(p, prevSlot);
=======
                int prevSlot = p.getInventory().getSelectedSlot();
                p.getInventory().setSelectedSlot(hotbarSlot);
                mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
                if (prevSlot != hotbarSlot) p.getInventory().setSelectedSlot(prevSlot);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            } else {
                InventoryUtility.quickMoveStack(mc, p, bestSlot);
            }
            lastEquipTime = System.currentTimeMillis();
            break;
        }
    }
<<<<<<< HEAD

    private void tickCustom(Minecraft mc, LocalPlayer p) {
        if (openInventory.getValue() && !(mc.screen instanceof InventoryScreen)) {
            return;
        }
        if (System.currentTimeMillis() - lastEquipTime < customDelay.getValue()) return;
        for (int armorIndex = 0; armorIndex < 4; armorIndex++) {
            EquipmentSlot equipSlot = ArmorUtility.getEquipmentSlotForIndex(armorIndex);
            var currentArmor = p.getItemBySlot(equipSlot);
            int bestSlot = ArmorUtility.findBestArmorSlot(p, armorIndex);
            if (bestSlot == -1) continue;
            var bestStack = InventoryUtility.getItem(p, bestSlot);
            if (!ArmorUtility.isArmorItem(bestStack) || !ArmorUtility.slotMatches(bestStack, equipSlot)) continue;
            if (onlyBetter.getValue() && !currentArmor.isEmpty()
                && !isBetterArmorIgnoreEnchants(bestStack, currentArmor, equipSlot)) continue;
            InventoryUtility.quickMoveStack(mc, p, bestSlot);
            lastEquipTime = System.currentTimeMillis();
            break;
        }
    }

    private boolean isBetterArmorIgnoreEnchants(net.minecraft.world.item.ItemStack a, net.minecraft.world.item.ItemStack b, EquipmentSlot slot) {
        if (!ArmorUtility.isArmorItem(a)) return false;
        if (!ArmorUtility.isArmorItem(b)) return true;
        if (ignoreEnchants.getValue()) {
            return ArmorUtility.getArmorScore(a, slot) > ArmorUtility.getArmorScore(b, slot);
        }
        return ArmorUtility.isBetterArmor(a, b, slot);
    }

=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    private int getLowestPriorityHotbarSlot(LocalPlayer p) {
        int worstSlot = 0;
        double worstScore = Double.MAX_VALUE;
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(p, i);
            double score = 0;
            if (!stack.isEmpty()) {
                if (ArmorUtility.isArmorItem(stack)) {
<<<<<<< HEAD
                    EquipmentSlot slot = InventoryUtility.getEquippableSlot(stack);
=======
                    EquipmentSlot slot = stack.get(DataComponents.EQUIPPABLE).slot();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                    score = ArmorUtility.getArmorScore(stack, slot);
                } else {
                    score = 1;
                }
            }
            if (score < worstScore) { worstScore = score; worstSlot = i; }
        }
        return worstSlot;
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(AutoArmor.class);
    }
    public static AutoArmor itz() {
        return ModuleManager.get(AutoArmor.class);
    }

}