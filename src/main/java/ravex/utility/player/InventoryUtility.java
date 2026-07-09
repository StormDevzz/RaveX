package ravex.utility.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
<<<<<<< HEAD
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.phys.EntityHitResult;
import java.util.function.Predicate;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
=======
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.phys.EntityHitResult;
import java.util.function.Predicate;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3

public class InventoryUtility {
    public static int inventorySlotToContainerSlot(int slot) {
        if (slot < 0 || slot > 35) return -1;
        return slot < 9 ? slot + 36 : slot;
    }

<<<<<<< HEAD
    public static int findSlot(net.minecraft.world.entity.player.Player player, Item target) {
=======
    public static int findSlot(LocalPlayer player, Item target) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        for (int i = 0; i < 36; i++) {
            if (player.getInventory().getItem(i).is(target)) return i;
        }
        return -1;
    }

<<<<<<< HEAD
    public static int findSlot(net.minecraft.world.entity.player.Player player, Predicate<ItemStack> predicate) {
=======
    public static int findSlot(LocalPlayer player, Predicate<ItemStack> predicate) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        for (int i = 0; i < 36; i++) {
            if (predicate.test(player.getInventory().getItem(i))) return i;
        }
        return -1;
    }

<<<<<<< HEAD
    public static int findSlot(net.minecraft.world.entity.player.Player player, Item target, int startInclusive, int endExclusive) {
=======
    public static int findSlot(LocalPlayer player, Item target, int startInclusive, int endExclusive) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        for (int i = startInclusive; i < endExclusive; i++) {
            if (player.getInventory().getItem(i).is(target)) return i;
        }
        return -1;
    }

<<<<<<< HEAD
    public static int findSlot(net.minecraft.world.entity.player.Player player, Predicate<ItemStack> predicate, int startInclusive, int endExclusive) {
=======
    public static int findSlot(LocalPlayer player, Predicate<ItemStack> predicate, int startInclusive, int endExclusive) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        for (int i = startInclusive; i < endExclusive; i++) {
            if (predicate.test(player.getInventory().getItem(i))) return i;
        }
        return -1;
    }

<<<<<<< HEAD
    public static int findHotbarSlot(net.minecraft.world.entity.player.Player player, Item target) {
        return findSlot(player, target, 0, 9);
    }

    public static int findEmptyHotbarSlot(net.minecraft.world.entity.player.Player player) {
=======
    public static int findHotbarSlot(LocalPlayer player, Item target) {
        return findSlot(player, target, 0, 9);
    }

    public static int findEmptyHotbarSlot(LocalPlayer player) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i).isEmpty()) return i;
        }
        return -1;
    }

<<<<<<< HEAD
    public static int countItem(net.minecraft.world.entity.player.Player player, Item target) {
=======
    public static int countItem(LocalPlayer player, Item target) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(target)) count += stack.getCount();
        }
        return count;
    }

<<<<<<< HEAD
    public static int countItem(net.minecraft.world.entity.player.Player player, String itemName) {
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isItem(stack, itemName)) count += stack.getCount();
        }
        return count;
    }

    public static ItemStack getMainHand(net.minecraft.world.entity.player.Player player) {
=======
    public static ItemStack getMainHand(LocalPlayer player) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        return player.getMainHandItem();
    }

    public static boolean isWeapon(Item item) {
        return item == Items.WOODEN_SWORD || item == Items.STONE_SWORD ||
               item == Items.IRON_SWORD || item == Items.GOLDEN_SWORD ||
               item == Items.DIAMOND_SWORD || item == Items.NETHERITE_SWORD ||
               item == Items.WOODEN_AXE || item == Items.STONE_AXE ||
               item == Items.IRON_AXE || item == Items.GOLDEN_AXE ||
               item == Items.DIAMOND_AXE || item == Items.NETHERITE_AXE ||
               item == Items.MACE;
    }

    public static Entity getHitEntity(Minecraft mc) {
        if (mc.hitResult instanceof EntityHitResult hit) return hit.getEntity();
        return null;
    }

    public static boolean isLookingAtEntity(Minecraft mc, Entity target, double range) {
        var result = mc.player.pick(range, 0.0f, false);
        return result instanceof EntityHitResult hit && hit.getEntity() == target;
    }

    public static void attackEntity(Minecraft mc, LivingEntity target, String swingMode) {
        mc.gameMode.attack(mc.player, target);
        if (swingMode.equals("Client")) mc.player.swing(InteractionHand.MAIN_HAND);
        else if (swingMode.equals("Server") && mc.player.connection != null)
            mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundSwingPacket(InteractionHand.MAIN_HAND));
    }

<<<<<<< HEAD
    public static boolean isBlockItem(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof net.minecraft.world.item.BlockItem;
    }

    public static boolean isHoldingBlock(net.minecraft.world.entity.player.Player player) {
=======
    public static boolean isHoldingBlock(LocalPlayer player) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        ItemStack stack = player.getMainHandItem();
        return !stack.isEmpty() && stack.getItem() instanceof net.minecraft.world.item.BlockItem;
    }

<<<<<<< HEAD
    public static boolean isHoldingItem(net.minecraft.world.entity.player.Player player) {
=======
    public static boolean isHoldingItem(LocalPlayer player) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        ItemStack stack = player.getMainHandItem();
        return !stack.isEmpty() && !(stack.getItem() instanceof net.minecraft.world.item.BlockItem);
    }

<<<<<<< HEAD
    public static int getSelectedSlot(net.minecraft.world.entity.player.Player player) {
        return player.getInventory().getSelectedSlot();
    }

    public static void selectSlot(net.minecraft.world.entity.player.Player player, int slot) {
=======
    public static int getSelectedSlot(LocalPlayer player) {
        return player.getInventory().getSelectedSlot();
    }

    public static void selectSlot(LocalPlayer player, int slot) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        player.getInventory().setSelectedSlot(slot);
    }

    public static void silentSelectSlot(LocalPlayer player, int slot) {
        if (player.connection != null)
            player.connection.send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(slot));
    }

    public static void swapToOffhand(Minecraft mc, LocalPlayer player, int inventorySlot) {
        int containerSlot = inventorySlotToContainerSlot(inventorySlot);
        if (containerSlot == -1) return;
        mc.gameMode.handleInventoryMouseClick(player.containerMenu.containerId, containerSlot, 0, ClickType.PICKUP, player);
        mc.gameMode.handleInventoryMouseClick(player.containerMenu.containerId, 45, 0, ClickType.PICKUP, player);
        mc.gameMode.handleInventoryMouseClick(player.containerMenu.containerId, containerSlot, 0, ClickType.PICKUP, player);
    }

    public static void quickMoveStack(Minecraft mc, LocalPlayer player, int inventorySlot) {
        int containerSlot = inventorySlotToContainerSlot(inventorySlot);
        if (containerSlot == -1) return;
        player.containerMenu.quickMoveStack(player, containerSlot);
    }

    public static void clickSlot(Minecraft mc, LocalPlayer player, int inventorySlot, int button, ClickType type) {
        int containerSlot = inventorySlotToContainerSlot(inventorySlot);
        if (containerSlot == -1) return;
        mc.gameMode.handleInventoryMouseClick(player.containerMenu.containerId, containerSlot, button, type, player);
    }
<<<<<<< HEAD

    public static void openInventoryScreen(LocalPlayer player) {
        Minecraft.getInstance().setScreen(new InventoryScreen(player));
    }

    public static void clickChestSlot(Minecraft mc, LocalPlayer player, int containerSlot, ClickType type) {
        mc.gameMode.handleInventoryMouseClick(player.containerMenu.containerId, containerSlot, 0, type, player);
    }

    public static void quickMoveSlot(Minecraft mc, int containerId, int slotIndex) {
        mc.gameMode.handleInventoryMouseClick(containerId, slotIndex, 0, ClickType.QUICK_MOVE, mc.player);
    }

    public static void swapSlots(Minecraft mc, int containerId, int slotA, int slotB) {
        mc.gameMode.handleInventoryMouseClick(containerId, slotA, slotB, ClickType.SWAP, mc.player);
    }

    public static int getItemUseCooldown(LocalPlayer player, ItemStack stack) {
        return player.getItemInHand(InteractionHand.MAIN_HAND).getItem() == stack.getItem()
            ? (int) player.getCurrentItemAttackStrengthDelay() : 0;
    }

    public static boolean isItemOnCooldown(LocalPlayer player, ItemStack stack) {
        return player.getCooldowns().isOnCooldown(stack);
    }

    public static ItemStack getOffhand(net.minecraft.world.entity.player.Player player) {
        return player.getOffhandItem();
    }

    public static boolean isHolding(net.minecraft.world.entity.player.Player player, Item item) {
        return player.getMainHandItem().is(item);
    }

    public static boolean isHolding(net.minecraft.world.entity.player.Player player, String itemName) {
        return isItem(player.getMainHandItem(), itemName);
    }

    public static boolean isOffhand(net.minecraft.world.entity.player.Player player, Item item) {
        return player.getOffhandItem().is(item);
    }

    public static boolean isOffhand(net.minecraft.world.entity.player.Player player, String itemName) {
        return isItem(player.getOffhandItem(), itemName);
    }

    public static ItemStack getItem(net.minecraft.world.entity.player.Player player, int slot) {
        return player.getInventory().getItem(slot);
    }

    public static int getContainerSize(net.minecraft.world.entity.player.Player player) {
        return player.getInventory().getContainerSize();
    }

    public static int containerMenuId(net.minecraft.world.entity.player.Player player) {
        return player.containerMenu.containerId;
    }

    public static void handleInventoryClick(Minecraft mc, LocalPlayer player, int containerSlot, int button, ClickType type) {
        mc.gameMode.handleInventoryMouseClick(player.containerMenu.containerId, containerSlot, button, type, player);
    }

    public static boolean isItemInSlot(net.minecraft.world.entity.player.Player player, int slot, String itemName) {
        ItemStack stack = getItem(player, slot);
        return !stack.isEmpty() && matchesItemId(stack, itemName);
    }

    public static boolean isItem(ItemStack stack, String itemName) {
        return !stack.isEmpty() && matchesItemId(stack, itemName);
    }

    public static boolean isAxeItem(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof net.minecraft.world.item.AxeItem;
    }

    public static boolean isShovelItem(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof net.minecraft.world.item.ShovelItem;
    }

    public static boolean isPickaxeItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return path.endsWith("_pickaxe");
    }

    public static boolean isSwordItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return path.endsWith("_sword");
    }

    public static boolean isToolItem(ItemStack stack) {
        return isAxeItem(stack) || isPickaxeItem(stack) || isShovelItem(stack) || isSwordItem(stack);
    }

    private static boolean matchesItemId(ItemStack stack, String itemName) {
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getPath().equals(itemName) || id.toString().equals(itemName);
    }

    public static int findHotbarSlot(net.minecraft.world.entity.player.Player player, String itemName) {
        for (int i = 0; i < 9; i++) {
            if (isItemInSlot(player, i, itemName)) return i;
        }
        return -1;
    }

    public static int findSlot(net.minecraft.world.entity.player.Player player, String itemName) {
        return findSlot(player, itemName, 0, 36);
    }

    public static int findSlot(net.minecraft.world.entity.player.Player player, String itemName, int start, int end) {
        for (int i = start; i < end; i++) {
            if (isItemInSlot(player, i, itemName)) return i;
        }
        return -1;
    }

    public static ItemEnchantments getEnchantments(ItemStack stack) {
        return stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
    }

    public static int getEnchantmentLevel(ItemStack stack, String enchantmentName) {
        ItemEnchantments enchants = getEnchantments(stack);
        for (Holder<Enchantment> holder : enchants.keySet()) {
            if (holder.getRegisteredName().equals(enchantmentName)) {
                return enchants.getLevel(holder);
            }
        }
        return 0;
    }

    public static boolean hasEnchantment(ItemStack stack, String enchantmentName) {
        return getEnchantmentLevel(stack, enchantmentName) > 0;
    }

    public static <T> T getComponent(ItemStack stack, net.minecraft.core.component.DataComponentType<? extends T> type) {
        return stack.get(type);
    }

    public static <T> T getComponentOrDefault(ItemStack stack, net.minecraft.core.component.DataComponentType<? extends T> type, T defaultValue) {
        return stack.getOrDefault(type, defaultValue);
    }

    public static boolean isTotem(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.TOTEM_OF_UNDYING);
    }

    public static boolean isCrystal(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.END_CRYSTAL);
    }

    public static boolean isAnchor(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.RESPAWN_ANCHOR);
    }

    public static boolean isGlowstone(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.GLOWSTONE);
    }

    public static boolean isGoldenApple(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.GOLDEN_APPLE);
    }

    public static boolean isEnchantedGoldenApple(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.ENCHANTED_GOLDEN_APPLE);
    }

    public static boolean isTrident(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.TRIDENT);
    }

    public static boolean isBow(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.BOW);
    }

    public static boolean isShulkerBox(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return path.contains("shulker_box");
    }

    public static boolean isPotion(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.POTION);
    }

    public static boolean isTippedArrow(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.TIPPED_ARROW);
    }

    public static net.minecraft.world.entity.EquipmentSlot getEquippableSlot(ItemStack stack) {
        var equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null ? equippable.slot() : null;
    }

    public static net.minecraft.world.food.FoodProperties getFoodProperties(ItemStack stack) {
        return stack.get(DataComponents.FOOD);
    }

    public static net.minecraft.world.item.alchemy.PotionContents getPotionContents(ItemStack stack) {
        return stack.get(DataComponents.POTION_CONTENTS);
    }

    public static net.minecraft.world.item.component.WrittenBookContent getWrittenBookContent(ItemStack stack) {
        return stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
    }

    public static void setWrittenBookContent(ItemStack stack, net.minecraft.world.item.component.WrittenBookContent content) {
        stack.set(DataComponents.WRITTEN_BOOK_CONTENT, content);
    }

    public static net.minecraft.world.item.component.WritableBookContent getWritableBookContent(ItemStack stack) {
        return stack.get(DataComponents.WRITABLE_BOOK_CONTENT);
    }

    public static boolean isWrittenBook(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.WRITTEN_BOOK);
    }

    public static boolean isWritableBook(ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.WRITABLE_BOOK);
    }
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
