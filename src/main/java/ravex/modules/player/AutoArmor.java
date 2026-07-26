package ravex.modules.player;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.parameter.BooleanParameter;
import ravex.parameter.DependencyParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.player.ArmorUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.SwingUtility;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import ravex.parameter.ModeParameter;
import java.util.List;




@ModuleInfo(name = "AutoArmor", category = "net.minecraft.world.entity.player.Player")
public class AutoArmor implements ModuleAccess {
    @Parameter(name = "Mode", modes = {"Normal", "Legit", "Custom"})
    public String mode = "Normal";
    @Parameter(name = "Delay", min = 0.0, max = 1000.0, step = 10.0)
    public double delay = 150.0;
    @Parameter(name = "OnlyBetter")
    public boolean onlyBetter = true;
    @Parameter(name = "Helmet")
    public boolean helmet = true;
    @Parameter(name = "Chestplate")
    public boolean chestplate = true;
    @Parameter(name = "Leggings")
    public boolean leggings = true;
    @Parameter(name = "Boots")
    public boolean boots = true;
    public final DependencyParameter<Double, NumberParameter> customDelay =
            new DependencyParameter<>(new NumberParameter("CustomDelay", 50.0, 0.0, 500.0, 10.0), new ModeParameter("Mode", "Normal", List.of("Normal", "Legit", "Custom")), "Custom");
    public final DependencyParameter<Boolean, BooleanParameter> openInventory =
            new DependencyParameter<>(new BooleanParameter("OpenInventory", true), new ModeParameter("Mode", "Normal", List.of("Normal", "Legit", "Custom")), "Custom");
    public final DependencyParameter<Boolean, BooleanParameter> ignoreEnchants =
            new DependencyParameter<>(new BooleanParameter("IgnoreEnchants", false), new ModeParameter("Mode", "Normal", List.of("Normal", "Legit", "Custom")), "Custom");
    private long lastEquipTime = 0;
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        net.minecraft.client.player.LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;
        String currentMode = mode;
        if ("Custom".equals(currentMode)) {
            tickCustom(mc, p);
        } else {
            tickNormal(mc, p, currentMode);
        }
    }

    private boolean isSlotEnabled(int armorIndex) {
        return switch (armorIndex) {
            case 0 -> helmet;
            case 1 -> chestplate;
            case 2 -> leggings;
            case 3 -> boots;
            default -> true;
        };
    }

    private void tickNormal(Minecraft mc, net.minecraft.client.player.LocalPlayer p, String currentMode) {
        if (mc.screen != null && !(mc.screen instanceof InventoryScreen)) return;
        if (System.currentTimeMillis() - lastEquipTime < delay) return;
        for (int armorIndex = 0; armorIndex < 4; armorIndex++) {
            if (!isSlotEnabled(armorIndex)) continue;
            EquipmentSlot equipSlot = ArmorUtility.getEquipmentSlotForIndex(armorIndex);
            var currentArmor = p.getItemBySlot(equipSlot);
            int bestSlot = ArmorUtility.findBestArmorSlot(p, armorIndex);
            if (bestSlot == -1) continue;
            var bestStack = InventoryUtility.getItem(p, bestSlot);
            if (!ArmorUtility.isArmorItem(bestStack) || !ArmorUtility.slotMatches(bestStack, equipSlot)) continue;
            if (onlyBetter && !currentArmor.isEmpty()
                && !ArmorUtility.isBetterArmor(bestStack, currentArmor, equipSlot)) continue;
            if ("Legit".equals(currentMode)) {
                int hotbarSlot = InventoryUtility.findEmptyHotbarSlot(p);
                if (hotbarSlot == -1) hotbarSlot = getBestHotbarSlot(p);
                if (hotbarSlot == -1 || hotbarSlot > 8) continue;
                InventoryUtility.quickMoveStack(mc, p, bestSlot);
                int prevSlot = InventoryUtility.getSelectedSlot(p);
                InventoryUtility.selectSlot(p, hotbarSlot);
                mc.gameMode.useItem(p, net.minecraft.world.InteractionHand.MAIN_HAND);
                if (prevSlot != hotbarSlot) InventoryUtility.selectSlot(p, prevSlot);
            } else {
                InventoryUtility.quickMoveStack(mc, p, bestSlot);
            }
            lastEquipTime = System.currentTimeMillis();
            break;
        }
    }

    private void tickCustom(Minecraft mc, net.minecraft.client.player.LocalPlayer p) {
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
            if (onlyBetter && !currentArmor.isEmpty()
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

    private int getBestHotbarSlot(net.minecraft.client.player.LocalPlayer p) {
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
        return ravex.manager.ModuleManager.INSTANCE.getByName("AutoArmor").getEnabled();
    }

    public static AutoArmor itz() {
        return ravex.manager.ModuleManager.delegate(AutoArmor.class);
    }


}