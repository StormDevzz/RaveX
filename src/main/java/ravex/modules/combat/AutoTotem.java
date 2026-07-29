package ravex.modules.combat;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.PlayerUtility;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;



@Module(name = "AutoTotem", category = "Combat")
public class AutoTotem {
    @Parameter(name = "Offhand", modes = {"Totem", "Gapple", "Crystal", "Shield", "None"})
    public String offhandItem = "Totem";
    @Parameter(name = "MainHand", modes = {"Sword", "Gapple", "Crystal", "Shield", "Totem", "None"})
    public String mainHandItem = "Sword";
    @Parameter(name = "MinHP", min = 1.0, max = 20.0, step = 0.5)
    public double minHealth = 8.0;
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var p = mc.getPlayer();
        if (p == null || mc.getGameMode() == null) return;
        boolean forceTotem = PlayerUtility.getHealth(p) <= minHealth;
        handleOffhand(mc, p, forceTotem);
        handleMainHand(mc, p, forceTotem);
    }
    private void handleOffhand(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer p, boolean forceTotem) {
        String choice = offhandItem;
        if (choice.equals("None") && !forceTotem) return;
        net.minecraft.world.item.Item targetItem = net.minecraft.world.item.Items.TOTEM_OF_UNDYING;
        if (!forceTotem) {
            if (choice.equals("Totem")) targetItem = net.minecraft.world.item.Items.TOTEM_OF_UNDYING;
            else if (choice.equals("Gapple")) targetItem = net.minecraft.world.item.Items.GOLDEN_APPLE;
            else if (choice.equals("Crystal")) targetItem = net.minecraft.world.item.Items.END_CRYSTAL;
            else if (choice.equals("Shield")) targetItem = net.minecraft.world.item.Items.SHIELD;
        }
        if (InventoryUtility.getOffhand(p).is(targetItem)) return;
        if (targetItem == net.minecraft.world.item.Items.TOTEM_OF_UNDYING && !InventoryUtility.getOffhand(p).isEmpty() && !InventoryUtility.isTotem(InventoryUtility.getOffhand(p))) return;
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
    private void handleMainHand(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer p, boolean forceTotem) {
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
        if (InventoryUtility.getMainHand(p).is(targetItem) || (targetItem == net.minecraft.world.item.Items.GOLDEN_APPLE && InventoryUtility.isEnchantedGoldenApple(InventoryUtility.getMainHand(p)))) {
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
    private void swapToOffhand(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer p, int invSlot) {
        int containerSlot = invSlot < 9 ? invSlot + 36 : invSlot;
        mc.getGameMode().handleInventoryMouseClick(p.containerMenu.containerId, containerSlot, 0, InventoryUtility.PICKUP, p);
        mc.getGameMode().handleInventoryMouseClick(p.containerMenu.containerId, 45, 0, InventoryUtility.PICKUP, p);
        mc.getGameMode().handleInventoryMouseClick(p.containerMenu.containerId, containerSlot, 0, InventoryUtility.PICKUP, p);
    }
    private int findSwordSlot() {
        var mc = MinecraftWrapper.getWrapper();
        var p = mc.getPlayer();
        for (int i = 0; i < 9; i++) {
            String name = InventoryUtility.getItem(p, i).getItem().toString().toLowerCase();
            if (name.contains("sword")) return i;
        }
        return -1;
    }




}