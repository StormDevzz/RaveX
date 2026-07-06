package ravex.modules.player;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import java.util.List;

public class AutoArmor extends Module {
    public static final AutoArmor INSTANCE = new AutoArmor();

    public final ModeParameter mode = new ModeParameter("Mode", "Normal",
            List.of("Normal", "Legit"));
    public final NumberParameter delay = new NumberParameter("Delay (ms)", 150.0, 0.0, 1000.0, 10.0);
    public final BooleanParameter onlyBetter = new BooleanParameter("Only Better", true);

    private long lastEquipTime = 0;

    private AutoArmor() {
        super("AutoArmor", Category.PLAYER);
        addParameter(mode);
        addParameter(delay);
        addParameter(onlyBetter);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;

        if (mc.screen != null && !(mc.screen instanceof InventoryScreen)) return;

        if (System.currentTimeMillis() - lastEquipTime < delay.getValue()) return;

        for (int armorIndex = 0; armorIndex < 4; armorIndex++) {
            EquipmentSlot equipSlot = getEquipmentSlotForIndex(armorIndex);
            ItemStack currentArmor = p.getItemBySlot(equipSlot);
            int bestSlot = findBestArmorSlot(p, armorIndex);

            if (bestSlot == -1) continue;

            ItemStack bestStack = p.getInventory().getItem(bestSlot);
            if (!isArmorItem(bestStack)) continue;

            if (!slotMatches(bestStack, equipSlot)) continue;

            if (onlyBetter.getValue() && !currentArmor.isEmpty()) {
                if (!isBetterArmor(bestStack, currentArmor, equipSlot)) continue;
            }

            if ("Legit".equals(mode.getValue())) {
                equipLegit(mc, p, bestSlot);
            } else {
                equipNormal(mc, p, bestSlot);
            }

            lastEquipTime = System.currentTimeMillis();
            break;
        }
    }

    private void equipNormal(Minecraft mc, LocalPlayer p, int inventorySlot) {
        int containerSlot = inventorySlotToContainerSlot(inventorySlot);
        if (containerSlot == -1) return;

        p.containerMenu.quickMoveStack(p, containerSlot);
    }

    private void equipLegit(Minecraft mc, LocalPlayer p, int inventorySlot) {
        int hotbarSlot = -1;
        if (inventorySlot < 9) {
            hotbarSlot = inventorySlot;
        } else {
            hotbarSlot = findEmptyHotbarSlot(p);
            if (hotbarSlot == -1) hotbarSlot = getLowestPriorityHotbarSlot(p);

            int fromContainerSlot = inventorySlotToContainerSlot(inventorySlot);
            int toContainerSlot = 36 + hotbarSlot;

            if (fromContainerSlot != -1 && toContainerSlot >= 36 && toContainerSlot <= 44) {
                p.containerMenu.quickMoveStack(p, fromContainerSlot);
                p.containerMenu.quickMoveStack(p, toContainerSlot);
            }
        }

        if (hotbarSlot == -1 || hotbarSlot > 8) return;

        int prevSlot = p.getInventory().getSelectedSlot();
        p.getInventory().setSelectedSlot(hotbarSlot);

        mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);

        if (prevSlot != hotbarSlot) {
            p.getInventory().setSelectedSlot(prevSlot);
        }
    }

    private int findBestArmorSlot(LocalPlayer p, int armorIndex) {
        EquipmentSlot equipSlot = getEquipmentSlotForIndex(armorIndex);
        int bestSlot = -1;
        ItemStack bestStack = ItemStack.EMPTY;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = p.getInventory().getItem(i);
            if (!isArmorItem(stack)) continue;

            if (!slotMatches(stack, equipSlot)) continue;

            if (bestSlot == -1 || isBetterArmor(stack, bestStack, equipSlot)) {
                bestSlot = i;
                bestStack = stack;
            }
        }

        return bestSlot;
    }

    private boolean isBetterArmor(ItemStack a, ItemStack b, EquipmentSlot slot) {
        if (!isArmorItem(a)) return false;
        if (!isArmorItem(b)) return true;
        return getArmorScore(a, slot) > getArmorScore(b, slot);
    }

    private boolean isArmorItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot().isArmor();
    }

    private boolean slotMatches(ItemStack stack, EquipmentSlot targetSlot) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot() == targetSlot;
    }

    private int getArmorScore(ItemStack stack, EquipmentSlot slot) {
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

    private EquipmentSlot getEquipmentSlotForIndex(int index) {
        return switch (index) {
            case 0 -> EquipmentSlot.FEET;
            case 1 -> EquipmentSlot.LEGS;
            case 2 -> EquipmentSlot.CHEST;
            case 3 -> EquipmentSlot.HEAD;
            default -> throw new IllegalArgumentException("Invalid armor index: " + index);
        };
    }

    private int inventorySlotToContainerSlot(int slot) {
        if (slot < 0 || slot > 35) return -1;
        return slot < 9 ? slot + 36 : slot;
    }

    private int findEmptyHotbarSlot(LocalPlayer p) {
        for (int i = 0; i < 9; i++) {
            if (p.getInventory().getItem(i).isEmpty()) return i;
        }
        return -1;
    }

    private int getLowestPriorityHotbarSlot(LocalPlayer p) {
        int worstSlot = 0;
        double worstScore = Double.MAX_VALUE;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = p.getInventory().getItem(i);
            double score = 0;
            if (!stack.isEmpty()) {
                if (isArmorItem(stack)) {
                    EquipmentSlot slot = stack.get(DataComponents.EQUIPPABLE).slot();
                    score = getArmorScore(stack, slot);
                } else {
                    score = 1;
                }
            }
            if (score < worstScore) {
                worstScore = score;
                worstSlot = i;
            }
        }
        return worstSlot;
    }
}
