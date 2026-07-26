package ravex.modules.combat;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.utility.player.InventoryUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ClickType;
import java.util.List;



@ModuleInfo(name = "AutoTotem", category = "Combat")
public class AutoTotem implements ModuleAccess {
    @Parameter(name = "Offhand", modes = {"Totem", "Gapple", "Crystal", "Shield", "None"})
    public String offhandItem = "Totem";
    @Parameter(name = "MainHand", modes = {"Sword", "Gapple", "Crystal", "Shield", "Totem", "None"})
    public String mainHandItem = "Sword";
    @Parameter(name = "MinHP", min = 1.0, max = 20.0, step = 0.5)
    public double minHealth = 8.0;
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null || mc.gameMode == null) return;
        boolean forceTotem = p.getHealth() <= minHealth;
        handleOffhand(mc, p, forceTotem);
        handleMainHand(mc, p, forceTotem);
    }
    private void handleOffhand(Minecraft mc, net.minecraft.client.player.LocalPlayer p, boolean forceTotem) {
        String choice = offhandItem;
        if (choice.equals("None") && !forceTotem) return;
        net.minecraft.world.item.Item targetItem = net.minecraft.world.item.Items.TOTEM_OF_UNDYING;
        if (!forceTotem) {
            if (choice.equals("Totem")) targetItem = net.minecraft.world.item.Items.TOTEM_OF_UNDYING;
            else if (choice.equals("Gapple")) targetItem = net.minecraft.world.item.Items.GOLDEN_APPLE;
            else if (choice.equals("Crystal")) targetItem = net.minecraft.world.item.Items.END_CRYSTAL;
            else if (choice.equals("Shield")) targetItem = net.minecraft.world.item.Items.SHIELD;
        }
        if (p.getOffhandItem().is(targetItem)) return;
        if (targetItem == net.minecraft.world.item.Items.TOTEM_OF_UNDYING && !p.getOffhandItem().isEmpty() && !InventoryUtility.isTotem(p.getOffhandItem())) return;
        int foundSlot = -1;
        for (int i = 0; i < 36; i++) {
            var stack = InventoryUtility.getItem(p, i);
            if (stack.is(targetItem) || (targetItem == net.minecraft.world.item.Items.GOLDEN_APPLE && InventoryUtility.isEnchantedGoldenApple(stack))) {
                foundSlot = i;
                break;
            }
        }
        if (foundSlot == -1 && !targetItem.equals(net.minecraft.world.item.Items.TOTEM_OF_UNDYING)) {
            for (int i = 0; i < 36; i++) {
                if (InventoryUtility.isTotem(InventoryUtility.getItem(p, i))) {
                    foundSlot = i;
                    break;
                }
            }
        }
        if (foundSlot != -1) {
            swapToOffhand(mc, p, foundSlot);
        }
    }
    private void handleMainHand(Minecraft mc, net.minecraft.client.player.LocalPlayer p, boolean forceTotem) {
        String mainChoice = mainHandItem;
        if (mainChoice.equals("None")) return;
        net.minecraft.world.item.Item targetItem = null;
        if (forceTotem) {
            targetItem = net.minecraft.world.item.Items.TOTEM_OF_UNDYING;
        } else {
            if (mainChoice.equals("Sword")) {
                int swordSlot = findSwordSlot();
                if (swordSlot != -1) {
                    InventoryUtility.selectSlot(p, swordSlot);
                    return;
                }
            } else if (mainChoice.equals("Gapple")) targetItem = net.minecraft.world.item.Items.GOLDEN_APPLE;
            else if (mainChoice.equals("Crystal")) targetItem = net.minecraft.world.item.Items.END_CRYSTAL;
            else if (mainChoice.equals("Shield")) targetItem = net.minecraft.world.item.Items.SHIELD;
            else if (mainChoice.equals("Totem")) targetItem = net.minecraft.world.item.Items.TOTEM_OF_UNDYING;
        }
        if (targetItem == null) return;
        if (p.getMainHandItem().is(targetItem) || (targetItem == net.minecraft.world.item.Items.GOLDEN_APPLE && InventoryUtility.isEnchantedGoldenApple(p.getMainHandItem()))) {
            return;
        }
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(p, i);
            if (stack.is(targetItem) || (targetItem == net.minecraft.world.item.Items.GOLDEN_APPLE && InventoryUtility.isEnchantedGoldenApple(stack))) {
                slot = i;
                break;
            }
        }
        if (slot != -1) {
            InventoryUtility.selectSlot(p, slot);
        }
    }
    private void swapToOffhand(Minecraft mc, net.minecraft.client.player.LocalPlayer p, int invSlot) {
        int containerSlot = invSlot < 9 ? invSlot + 36 : invSlot;
        mc.gameMode.handleInventoryMouseClick(p.containerMenu.containerId, containerSlot, 0, ClickType.PICKUP, p);
        mc.gameMode.handleInventoryMouseClick(p.containerMenu.containerId, 45, 0, ClickType.PICKUP, p);
        mc.gameMode.handleInventoryMouseClick(p.containerMenu.containerId, containerSlot, 0, ClickType.PICKUP, p);
    }
    private int findSwordSlot() {
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        for (int i = 0; i < 9; i++) {
            String name = InventoryUtility.getItem(p, i).getItem().toString().toLowerCase();
            if (name.contains("sword")) return i;
        }
        return -1;
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("AutoTotem").getEnabled();
    }
    public static AutoTotem itz() {
        return ravex.manager.ModuleManager.delegate(AutoTotem.class);
    }


}