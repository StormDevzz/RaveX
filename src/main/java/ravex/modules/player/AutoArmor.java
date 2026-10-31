package ravex.modules.player;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.PotionUtility;
import ravex.utility.misc.ScreenUtility;
import ravex.utility.player.ArmorUtility;
import ravex.utility.player.InventoryUtility;
import ravex.mcwrapper.MinecraftWrapper;

@Module(name = "AutoArmor", category = "Player")
public class AutoArmor {
    // legit: opens the inventory and equips there
    @Parameter(name = "OpenInventory")
    public boolean openInventory = false;
    @Parameter(name = "Delay", min = 0.0, max = 1000.0, step = 10.0)
    public double delay = 150.0;
    @Parameter(name = "OnlyBetter")
    public boolean onlyBetter = true;
    @Parameter(name = "IgnoreEnchants")
    public boolean ignoreEnchants = false;
    @Parameter(name = "Helmet")
    public boolean helmet = true;
    @Parameter(name = "Chestplate")
    public boolean chestplate = true;
    @Parameter(name = "Leggings")
    public boolean leggings = true;
    @Parameter(name = "Boots")
    public boolean boots = true;

    private long lastEquipTime = 0;
    private boolean openedByModule = false;

    public void onDisable() {
        openedByModule = false;
    }

    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        net.minecraft.client.player.LocalPlayer p = mc.getPlayer();
        if (p == null || mc.getLevel() == null) return;
        var screen = mc.getCurrentScreen();

        if (openInventory) {
            if (screen == null || ScreenUtility.isCreativeInventoryScreen(mc)) {
                openedByModule = false;
                if (findUpgrade(p) != null) {
                    ensurePlayerMenu(p);
                    InventoryUtility.openInventoryScreen(p);
                    openedByModule = true;
                }
                return;
            }
            if (!ScreenUtility.isPlayerInventoryScreen(mc)) return;
            if (findUpgrade(p) == null) {
                if (openedByModule) {
                    ScreenUtility.closeScreen(mc);
                    openedByModule = false;
                }
                return;
            }
            ensurePlayerMenu(p);
            equipNext(mc, p);
            return;
        }

        if (screen != null && !ScreenUtility.isPlayerInventoryScreen(mc)) return;
        if (findUpgrade(p) == null) return;
        ensurePlayerMenu(p);
        equipNext(mc, p);
    }

    // a stale creative menu swallows the clicks
    private void ensurePlayerMenu(net.minecraft.client.player.LocalPlayer p) {
        if (p.containerMenu != p.inventoryMenu && p.containerMenu.containerId == 0) {
            p.containerMenu = p.inventoryMenu;
        }
    }

    private void equipNext(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer p) {
        if (System.currentTimeMillis() - lastEquipTime < delay) return;
        int[] up = findUpgrade(p);
        if (up == null) return;
        equipArmor(mc, p, up[1], ArmorUtility.getEquipmentSlotForIndex(up[0]));
        lastEquipTime = System.currentTimeMillis();
    }

    // {armorIndex, inventorySlot} or null when none
    private int[] findUpgrade(net.minecraft.client.player.LocalPlayer p) {
        for (int armorIndex = 0; armorIndex < 4; armorIndex++) {
            if (!isSlotEnabled(armorIndex)) continue;
            var equipSlot = ArmorUtility.getEquipmentSlotForIndex(armorIndex);
            var currentArmor = p.getItemBySlot(equipSlot);
            int bestSlot = ArmorUtility.findBestArmorSlot(p, armorIndex);
            if (bestSlot == -1) continue;
            var bestStack = InventoryUtility.getItem(p, bestSlot);
            if (!ArmorUtility.isArmorItem(bestStack) || !ArmorUtility.slotMatches(bestStack, equipSlot)) continue;
            if (onlyBetter && !currentArmor.isEmpty() && !isBetter(bestStack, currentArmor, equipSlot)) continue;
            return new int[]{armorIndex, bestSlot};
        }
        return null;
    }

    private boolean isBetter(net.minecraft.world.item.ItemStack candidate, net.minecraft.world.item.ItemStack worn,
                             net.minecraft.world.entity.EquipmentSlot slot) {
        if (ignoreEnchants) {
            int candidateDef = PotionUtility.getArmorDefense(candidate, slot);
            int wornDef = PotionUtility.getArmorDefense(worn, slot);
            return candidateDef > wornDef;
        }
        return ArmorUtility.isBetterArmor(candidate, worn, slot);
    }

    private boolean isSlotEnabled(int armorIndex) {
        return switch (armorIndex) {
            // index order: 0 feet, 1 legs, 2 chest, 3 head
            case 0 -> boots;
            case 1 -> leggings;
            case 2 -> chestplate;
            case 3 -> helmet;
            default -> true;
        };
    }

    private int armorContainerSlot(net.minecraft.world.entity.EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> 5;
            case CHEST -> 6;
            case LEGS -> 7;
            case FEET -> 8;
            default -> -1;
        };
    }

    // quick move will not replace worn armor, three click swap
    private void equipArmor(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer p, int invSlot,
                            net.minecraft.world.entity.EquipmentSlot equipSlot) {
        int armorSlot = armorContainerSlot(equipSlot);
        if (armorSlot == -1) return;
        int from = InventoryUtility.inventorySlotToContainerSlot(invSlot);
        if (from == -1) return;
        int menuId = p.containerMenu.containerId;
        var gm = mc.getGameMode();
        if (gm == null) return;
        // works for an empty slot too
        gm.handleInventoryMouseClick(menuId, armorSlot, 0, InventoryUtility.PICKUP, p);
        gm.handleInventoryMouseClick(menuId, from, 0, InventoryUtility.PICKUP, p);
        gm.handleInventoryMouseClick(menuId, armorSlot, 0, InventoryUtility.PICKUP, p);
    }
}
