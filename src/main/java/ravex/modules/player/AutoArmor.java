package ravex.modules.player;

import ravex.manager.ModuleManager;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import java.util.List;

public class AutoArmor extends Module {
    public final ModeParameter mode = new ModeParameter("Mode", "Normal",
            List.of("Normal", "Legit", "Custom"));
    public final NumberParameter delay = new NumberParameter("Delay", 150.0, 0.0, 1000.0, 10.0);
    public final BooleanParameter onlyBetter = new BooleanParameter("OnlyBetter", true);
    public final BooleanParameter helmet = new BooleanParameter("Helmet", true);
    public final BooleanParameter chestplate = new BooleanParameter("Chestplate", true);
    public final BooleanParameter leggings = new BooleanParameter("Leggings", true);
    public final BooleanParameter boots = new BooleanParameter("Boots", true);
    public final DependencyParameter<Double, NumberParameter> customDelay =
            new DependencyParameter<>(new NumberParameter("CustomDelay", 50.0, 0.0, 500.0, 10.0), mode, "Custom");
    public final DependencyParameter<Boolean, BooleanParameter> openInventory =
            new DependencyParameter<>(new BooleanParameter("OpenInventory", true), mode, "Custom");
    public final DependencyParameter<Boolean, BooleanParameter> ignoreEnchants =
            new DependencyParameter<>(new BooleanParameter("IgnoreEnchants", false), mode, "Custom");
    private long lastEquipTime = 0;

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;
        String currentMode = mode.getValue();
        if ("Custom".equals(currentMode)) {
            tickCustom(mc, p);
        } else {
            tickNormal(mc, p, currentMode);
        }
    }

    private boolean isSlotEnabled(int armorIndex) {
        return switch (armorIndex) {
            case 0 -> helmet.getValue();
            case 1 -> chestplate.getValue();
            case 2 -> leggings.getValue();
            case 3 -> boots.getValue();
            default -> true;
        };
    }

    private void tickNormal(Minecraft mc, LocalPlayer p, String currentMode) {
        if (mc.screen != null && !(mc.screen instanceof InventoryScreen)) return;
        if (System.currentTimeMillis() - lastEquipTime < delay.getValue()) return;
        for (int armorIndex = 0; armorIndex < 4; armorIndex++) {
            if (!isSlotEnabled(armorIndex)) continue;
            EquipmentSlot equipSlot = ArmorUtility.getEquipmentSlotForIndex(armorIndex);
            var currentArmor = p.getItemBySlot(equipSlot);
            int bestSlot = ArmorUtility.findBestArmorSlot(p, armorIndex);
            if (bestSlot == -1) continue;
            var bestStack = InventoryUtility.getItem(p, bestSlot);
            if (!ArmorUtility.isArmorItem(bestStack) || !ArmorUtility.slotMatches(bestStack, equipSlot)) continue;
            if (onlyBetter.getValue() && !currentArmor.isEmpty()
                && !ArmorUtility.isBetterArmor(bestStack, currentArmor, equipSlot)) continue;
            if ("Legit".equals(currentMode)) {
                int hotbarSlot = InventoryUtility.findEmptyHotbarSlot(p);
                if (hotbarSlot == -1) hotbarSlot = getBestHotbarSlot(p);
                if (hotbarSlot == -1 || hotbarSlot > 8) continue;
                InventoryUtility.quickMoveStack(mc, p, bestSlot);
                int prevSlot = InventoryUtility.getSelectedSlot(p);
                InventoryUtility.selectSlot(p, hotbarSlot);
                mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
                if (prevSlot != hotbarSlot) InventoryUtility.selectSlot(p, prevSlot);
            } else {
                InventoryUtility.quickMoveStack(mc, p, bestSlot);
            }
            lastEquipTime = System.currentTimeMillis();
            break;
        }
    }

    private void tickCustom(Minecraft mc, LocalPlayer p) {
        if (openInventory.getValue() && !(mc.screen instanceof InventoryScreen)) {
            return;
        }
        if (System.currentTimeMillis() - lastEquipTime < customDelay.getValue()) return;
        for (int armorIndex = 0; armorIndex < 4; armorIndex++) {
            if (!isSlotEnabled(armorIndex)) continue;
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

    private boolean isBetterArmorIgnoreEnchants(net.minecraft.world.item.ItemStack a,
            net.minecraft.world.item.ItemStack b, EquipmentSlot slot) {
        if (!ArmorUtility.isArmorItem(a)) return false;
        if (!ArmorUtility.isArmorItem(b)) return true;
        if (ignoreEnchants.getValue()) {
            return ArmorUtility.getArmorScore(a, slot) > ArmorUtility.getArmorScore(b, slot);
        }
        return ArmorUtility.isBetterArmor(a, b, slot);
    }

    private int getBestHotbarSlot(LocalPlayer p) {
        int bestSlot = -1;
        double bestScore = Double.MIN_VALUE;
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(p, i);
            double score;
            if (stack.isEmpty()) {
                score = 100;
            } else if (ArmorUtility.isArmorItem(stack)) {
                EquipmentSlot slot = InventoryUtility.getEquippableSlot(stack);
                score = ArmorUtility.getArmorScore(stack, slot);
            } else {
                score = 0;
            }
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(AutoArmor.class);
    }

    public static AutoArmor itz() {
        return ModuleManager.get(AutoArmor.class);
    }
}
