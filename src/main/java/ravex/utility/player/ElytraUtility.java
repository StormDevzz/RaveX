package ravex.utility.player;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.EquipmentSlot;
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

    private static boolean isBetterChestplate(ItemStack a, ItemStack b) {
        if (b.isEmpty()) return true;
        var modA = a.get(DataComponents.ATTRIBUTE_MODIFIERS);
        var modB = b.get(DataComponents.ATTRIBUTE_MODIFIERS);
        int defA = modA != null ? (int) modA.compute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR, 0.0, EquipmentSlot.CHEST) : 0;
        int defB = modB != null ? (int) modB.compute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR, 0.0, EquipmentSlot.CHEST) : 0;
        return defA > defB;
    }
}
