package ravex.utility.player;

<<<<<<< HEAD
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ClickType;
=======
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.EquipmentSlot;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import net.minecraft.core.component.DataComponents;

public class ElytraUtility {
    public static boolean isElytraEquipped(LocalPlayer player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
    }

    public static int getDurability(ItemStack stack) {
        return stack.getMaxDamage() - stack.getDamageValue();
    }

    public static int getElytraDurability(LocalPlayer player) {
        return getDurability(player.getItemBySlot(EquipmentSlot.CHEST));
    }

    public static boolean hasElytraDurability(LocalPlayer player, int minDurability) {
        return isElytraEquipped(player) && getElytraDurability(player) >= minDurability;
    }

    public static int findElytraSlot(LocalPlayer player) {
        return InventoryUtility.findSlot(player, Items.ELYTRA);
    }

    public static int findElytraSlot(LocalPlayer player, int minDurability) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.ELYTRA) && getDurability(stack) >= minDurability) return i;
        }
        return -1;
    }

    public static int findChestplateSlot(LocalPlayer player) {
        int best = -1;
        ItemStack bestStack = ItemStack.EMPTY;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || stack.is(Items.ELYTRA)) continue;
            var equippable = stack.get(DataComponents.EQUIPPABLE);
            if (equippable != null && equippable.slot() == EquipmentSlot.CHEST) {
                if (best == -1 || isBetterChestplate(stack, bestStack)) {
                    best = i;
                    bestStack = stack;
                }
            }
        }
        return best;
    }

<<<<<<< HEAD
    public static boolean useFirework(LocalPlayer player) {
        if (player == null || !player.isFallFlying()) return false;
        Minecraft mc = Minecraft.getInstance();
        int prevSlot = player.getInventory().getSelectedSlot();
        int slot = InventoryUtility.findSlot(player, Items.FIREWORK_ROCKET);
        if (slot == -1) return false;
        player.getInventory().setSelectedSlot(slot);
        mc.gameMode.useItem(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        player.getInventory().setSelectedSlot(prevSlot);
        return true;
    }

    public static void setPitch(LocalPlayer player, float pitch) {
        if (player != null) player.setXRot(pitch);
    }

    public static boolean swapToChestplate(LocalPlayer player) {
        if (player == null || !isElytraEquipped(player)) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null) return false;
        int slot = findChestplateSlot(player);
        if (slot == -1) return false;
        int containerSlot = InventoryUtility.inventorySlotToContainerSlot(slot);
        if (containerSlot == -1) return false;
        mc.gameMode.handleInventoryMouseClick(
            player.containerMenu.containerId, 6, 0, ClickType.PICKUP, player);
        mc.gameMode.handleInventoryMouseClick(
            player.containerMenu.containerId, containerSlot, 0, ClickType.PICKUP, player);
        mc.gameMode.handleInventoryMouseClick(
            player.containerMenu.containerId, 6, 0, ClickType.PICKUP, player);
        return true;
    }

    public static boolean isFallFlying(LocalPlayer player) {
        return player != null && player.isFallFlying();
    }

=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    private static boolean isBetterChestplate(ItemStack a, ItemStack b) {
        if (b.isEmpty()) return true;
        var modA = a.get(DataComponents.ATTRIBUTE_MODIFIERS);
        var modB = b.get(DataComponents.ATTRIBUTE_MODIFIERS);
        int defA = modA != null ? (int) modA.compute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR, 0.0, EquipmentSlot.CHEST) : 0;
        int defB = modB != null ? (int) modB.compute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR, 0.0, EquipmentSlot.CHEST) : 0;
        return defA > defB;
    }
}
