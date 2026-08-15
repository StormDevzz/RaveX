package ravex.modules.player;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.player.ArmorUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.SwingUtility;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.EquipmentSlot;
import ravex.mcwrapper.MinecraftWrapper;




@Module(name = "AutoArmor", category = "Player")
public class AutoArmor {
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
    @Parameter(name = "CustomDelay", min = 0.0, max = 500.0, step = 10.0, visible = "mode=Custom")
    public double customDelay = 50.0;
    @Parameter(name = "OpenInventory", visible = "mode=Custom")
    public boolean openInventory = true;
    @Parameter(name = "IgnoreEnchants", visible = "mode=Custom")
    public boolean ignoreEnchants = false;
    private long lastEquipTime = 0;
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        net.minecraft.client.player.LocalPlayer p = mc.getPlayer();
        if (p == null || mc.getLevel() == null) return;
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

    private void tickNormal(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer p, String currentMode) {
        if (mc.getCurrentScreen() != null && !(mc.getCurrentScreen() instanceof InventoryScreen)) return;
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
                mc.getGameMode().useItem(p, net.minecraft.world.InteractionHand.MAIN_HAND);
                if (prevSlot != hotbarSlot) InventoryUtility.selectSlot(p, prevSlot);
            } else {
                InventoryUtility.quickMoveStack(mc, p, bestSlot);
            }
            lastEquipTime = System.currentTimeMillis();
            break;
        }
    }

    private void tickCustom(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer p) {
        if (openInventory && !(mc.getCurrentScreen() instanceof InventoryScreen)) {
            return;
        }
        if (System.currentTimeMillis() - lastEquipTime < customDelay) return;
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
        if (ignoreEnchants) {
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






}