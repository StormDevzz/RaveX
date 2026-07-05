package ravex.utility.player;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class ArmorUtility {
    public static boolean isArmorItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot().isArmor();
    }

    public static boolean slotMatches(ItemStack stack, EquipmentSlot targetSlot) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot() == targetSlot;
    }

    public static EquipmentSlot getEquipmentSlotForIndex(int index) {
        return switch (index) {
            case 0 -> EquipmentSlot.FEET;
            case 1 -> EquipmentSlot.LEGS;
            case 2 -> EquipmentSlot.CHEST;
            case 3 -> EquipmentSlot.HEAD;
            default -> throw new IllegalArgumentException("Invalid armor index: " + index);
        };
    }

    public static int getArmorScore(ItemStack stack, EquipmentSlot slot) {
        if (!isArmorItem(stack)) return 0;
        int defense = 0;
        ItemAttributeModifiers modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers != null) {
            defense = (int) modifiers.compute(Attributes.ARMOR, 0.0, slot);
        }
        int protLevel = 0;
        ItemEnchantments enchants = stack.get(DataComponents.ENCHANTMENTS);
        if (enchants != null) {
            for (var enchantment : enchants.keySet()) {
                String id = enchantment.getRegisteredName().toLowerCase();
                if (id.contains("protection") || id.contains("projectile_protection")
                        || id.contains("blast_protection") || id.contains("fire_protection")) {
                    protLevel += enchants.getLevel(enchantment);
                }
            }
        }
        return defense * 100 + protLevel * 10;
    }

    public static boolean isBetterArmor(ItemStack a, ItemStack b, EquipmentSlot slot) {
        if (!isArmorItem(a)) return false;
        if (!isArmorItem(b)) return true;
        return getArmorScore(a, slot) > getArmorScore(b, slot);
    }

    public static int findBestArmorSlot(LocalPlayer player, int armorIndex) {
        EquipmentSlot equipSlot = getEquipmentSlotForIndex(armorIndex);
        int bestSlot = -1;
        ItemStack bestStack = ItemStack.EMPTY;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!isArmorItem(stack)) continue;
            if (!slotMatches(stack, equipSlot)) continue;
            if (bestSlot == -1 || isBetterArmor(stack, bestStack, equipSlot)) {
                bestSlot = i;
                bestStack = stack;
            }
        }
        return bestSlot;
    }
}
