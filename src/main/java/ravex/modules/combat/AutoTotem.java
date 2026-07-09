package ravex.modules.combat;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
=======
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
<<<<<<< HEAD
import net.minecraft.world.inventory.ClickType;
import java.util.List;
import ravex.utility.player.InventoryUtility;
public class AutoTotem extends Module {
=======
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.inventory.ClickType;
import java.util.List;
public class AutoTotem extends Module {
    public static final AutoTotem INSTANCE = new AutoTotem();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final ModeParameter offhandItem = new ModeParameter("Offhand", "Totem", List.of("Totem", "Gapple", "Crystal", "Shield", "None"));
    public final ModeParameter mainHandItem = new ModeParameter("MainHand", "Sword", List.of("Sword", "Gapple", "Crystal", "Shield", "Totem", "None"));
    public final NumberParameter minHealth = new NumberParameter("MinHP", 8.0, 1.0, 20.0, 0.5);

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null || mc.gameMode == null) return;
        boolean forceTotem = p.getHealth() <= minHealth.getValue();
        handleOffhand(mc, p, forceTotem);
        handleMainHand(mc, p, forceTotem);
    }
    private void handleOffhand(Minecraft mc, LocalPlayer p, boolean forceTotem) {
        String choice = offhandItem.getValue();
        if (choice.equals("None") && !forceTotem) return;
<<<<<<< HEAD
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
=======
        Item targetItem = Items.TOTEM_OF_UNDYING;
        if (!forceTotem) {
            if (choice.equals("Totem")) targetItem = Items.TOTEM_OF_UNDYING;
            else if (choice.equals("Gapple")) targetItem = Items.GOLDEN_APPLE;
            else if (choice.equals("Crystal")) targetItem = Items.END_CRYSTAL;
            else if (choice.equals("Shield")) targetItem = Items.SHIELD;
        }
        if (p.getOffhandItem().is(targetItem)) return;
        if (targetItem == Items.TOTEM_OF_UNDYING && !p.getOffhandItem().isEmpty() && !p.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return;
        int foundSlot = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = p.getInventory().getItem(i);
            if (stack.is(targetItem) || (targetItem == Items.GOLDEN_APPLE && stack.is(Items.ENCHANTED_GOLDEN_APPLE))) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                foundSlot = i;
                break;
            }
        }
<<<<<<< HEAD
        if (foundSlot == -1 && !targetItem.equals(net.minecraft.world.item.Items.TOTEM_OF_UNDYING)) {
            for (int i = 0; i < 36; i++) {
                if (InventoryUtility.isTotem(InventoryUtility.getItem(p, i))) {
=======
        if (foundSlot == -1 && !targetItem.equals(Items.TOTEM_OF_UNDYING)) {
            for (int i = 0; i < 36; i++) {
                if (p.getInventory().getItem(i).is(Items.TOTEM_OF_UNDYING)) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                    foundSlot = i;
                    break;
                }
            }
        }
        if (foundSlot != -1) {
            swapToOffhand(mc, p, foundSlot);
        }
    }
    private void handleMainHand(Minecraft mc, LocalPlayer p, boolean forceTotem) {
        String mainChoice = mainHandItem.getValue();
        if (mainChoice.equals("None")) return;
<<<<<<< HEAD
        net.minecraft.world.item.Item targetItem = null;
        if (forceTotem) {
            targetItem = net.minecraft.world.item.Items.TOTEM_OF_UNDYING;
=======
        Item targetItem = Items.AIR;
        if (forceTotem) {
            targetItem = Items.TOTEM_OF_UNDYING;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        } else {
            if (mainChoice.equals("Sword")) {
                int swordSlot = findSwordSlot();
                if (swordSlot != -1) {
<<<<<<< HEAD
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
=======
                    p.getInventory().setSelectedSlot(swordSlot);
                    return;
                }
            } else if (mainChoice.equals("Gapple")) targetItem = Items.GOLDEN_APPLE;
            else if (mainChoice.equals("Crystal")) targetItem = Items.END_CRYSTAL;
            else if (mainChoice.equals("Shield")) targetItem = Items.SHIELD;
            else if (mainChoice.equals("Totem")) targetItem = Items.TOTEM_OF_UNDYING;
        }
        if (targetItem == Items.AIR) return;
        if (p.getMainHandItem().is(targetItem) || (targetItem == Items.GOLDEN_APPLE && p.getMainHandItem().is(Items.ENCHANTED_GOLDEN_APPLE))) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            return;
        }
        int slot = -1;
        for (int i = 0; i < 9; i++) {
<<<<<<< HEAD
            var stack = InventoryUtility.getItem(p, i);
            if (stack.is(targetItem) || (targetItem == net.minecraft.world.item.Items.GOLDEN_APPLE && InventoryUtility.isEnchantedGoldenApple(stack))) {
=======
            ItemStack stack = p.getInventory().getItem(i);
            if (stack.is(targetItem) || (targetItem == Items.GOLDEN_APPLE && stack.is(Items.ENCHANTED_GOLDEN_APPLE))) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                slot = i;
                break;
            }
        }
        if (slot != -1) {
<<<<<<< HEAD
            InventoryUtility.selectSlot(p, slot);
=======
            p.getInventory().setSelectedSlot(slot);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        }
    }
    private void swapToOffhand(Minecraft mc, LocalPlayer p, int invSlot) {
        int containerSlot = invSlot < 9 ? invSlot + 36 : invSlot;
        mc.gameMode.handleInventoryMouseClick(p.containerMenu.containerId, containerSlot, 0, ClickType.PICKUP, p);
        mc.gameMode.handleInventoryMouseClick(p.containerMenu.containerId, 45, 0, ClickType.PICKUP, p);
        mc.gameMode.handleInventoryMouseClick(p.containerMenu.containerId, containerSlot, 0, ClickType.PICKUP, p);
    }
    private int findSwordSlot() {
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        for (int i = 0; i < 9; i++) {
<<<<<<< HEAD
            String name = InventoryUtility.getItem(p, i).getItem().toString().toLowerCase();
=======
            String name = p.getInventory().getItem(i).getItem().toString().toLowerCase();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            if (name.contains("sword")) return i;
        }
        return -1;
    }
<<<<<<< HEAD
    public static boolean maybeEnabled() {
        return maybeEnabled(AutoTotem.class);
    }
    public static AutoTotem itz() {
        return ModuleManager.get(AutoTotem.class);
    }

}
=======
}
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
