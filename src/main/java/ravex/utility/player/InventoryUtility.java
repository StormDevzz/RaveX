package ravex.utility.player;

import ravex.mcwrapper.MinecraftWrapper;
import ravex.utility.network.NetworkUtility;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.Identifier;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.phys.EntityHitResult;
import java.util.function.Predicate;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.inventory.ClickType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public class InventoryUtility {
    public static final ClickType PICKUP = ClickType.PICKUP;
    public static final ClickType QUICK_MOVE = ClickType.QUICK_MOVE;
    public static final ClickType SWAP = ClickType.SWAP;

    public static int inventorySlotToContainerSlot(int slot) {
        if (slot < 0 || slot > 35) return -1;
        return slot < 9 ? slot + 36 : slot;
    }

    @Range(from = -1, to = 35)
    public static int findSlot(@NotNull net.minecraft.world.entity.player.Player player, @NotNull Item target) {
        for (int i = 0; i < 36; i++) {
            if (player.getInventory().getItem(i).is(target)) return i;
        }
        return -1;
    }

    @Range(from = -1, to = 35)
    public static int findSlot(@NotNull net.minecraft.world.entity.player.Player player, @NotNull Predicate<ItemStack> predicate) {
        for (int i = 0; i < 36; i++) {
            if (predicate.test(player.getInventory().getItem(i))) return i;
        }
        return -1;
    }

    @Range(from = -1, to = 35)
    public static int findSlot(@NotNull net.minecraft.world.entity.player.Player player, @NotNull Item target, int startInclusive, int endExclusive) {
        for (int i = startInclusive; i < endExclusive; i++) {
            if (player.getInventory().getItem(i).is(target)) return i;
        }
        return -1;
    }

    @Range(from = -1, to = 35)
    public static int findSlot(@NotNull net.minecraft.world.entity.player.Player player, @NotNull Predicate<ItemStack> predicate, int startInclusive, int endExclusive) {
        for (int i = startInclusive; i < endExclusive; i++) {
            if (predicate.test(player.getInventory().getItem(i))) return i;
        }
        return -1;
    }

    @Range(from = -1, to = 8)
    public static int findHotbarSlot(@NotNull net.minecraft.world.entity.player.Player player, @NotNull Item target) {
        return findSlot(player, target, 0, 9);
    }

    @Range(from = -1, to = 8)
    public static int findEmptyHotbarSlot(@NotNull net.minecraft.world.entity.player.Player player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i).isEmpty()) return i;
        }
        return -1;
    }

    public static int countItem(@NotNull net.minecraft.world.entity.player.Player player, @NotNull Item target) {
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(target)) count += stack.getCount();
        }
        return count;
    }

    public static int countItem(@NotNull net.minecraft.world.entity.player.Player player, @NotNull String itemName) {
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isItem(stack, itemName)) count += stack.getCount();
        }
        return count;
    }

    @NotNull
    public static ItemStack getMainHand(@NotNull net.minecraft.world.entity.player.Player player) {
        return player.getMainHandItem();
    }

    public static boolean isWeapon(@NotNull Item item) {
        return item == Items.WOODEN_SWORD || item == Items.STONE_SWORD ||
               item == Items.IRON_SWORD || item == Items.GOLDEN_SWORD ||
               item == Items.DIAMOND_SWORD || item == Items.NETHERITE_SWORD ||
               item == Items.WOODEN_AXE || item == Items.STONE_AXE ||
               item == Items.IRON_AXE || item == Items.GOLDEN_AXE ||
               item == Items.DIAMOND_AXE || item == Items.NETHERITE_AXE ||
               item == Items.MACE;
    }

    @Nullable
    public static Entity getHitEntity(@NotNull MinecraftWrapper mc) {
        var _mc = mc.getRaw();
        if (_mc.hitResult instanceof EntityHitResult hit) return hit.getEntity();
        return null;
    }

    public static boolean isLookingAtEntity(@NotNull MinecraftWrapper mc, @NotNull Entity target, double range) {
        var _mc = mc.getRaw();
        var result = _mc.player.pick(range, 0.0f, false);
        return result instanceof EntityHitResult hit && hit.getEntity() == target;
    }

    public static void attackEntity(@NotNull MinecraftWrapper mc, @NotNull LivingEntity target, @NotNull String swingMode) {
        var _mc = mc.getRaw();
        _mc.gameMode.attack(_mc.player, target);
        if (swingMode.equals("Client")) _mc.player.swing(InteractionHand.MAIN_HAND);
        else if (swingMode.equals("Server") && _mc.player.connection != null)
            NetworkUtility.sendPacket(new net.minecraft.network.protocol.game.ServerboundSwingPacket(InteractionHand.MAIN_HAND));
    }

    public static boolean isBlockItem(@NotNull ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof net.minecraft.world.item.BlockItem;
    }

    public static boolean isHoldingBlock(@NotNull net.minecraft.world.entity.player.Player player) {
        ItemStack stack = player.getMainHandItem();
        return !stack.isEmpty() && stack.getItem() instanceof net.minecraft.world.item.BlockItem;
    }

    public static boolean isHoldingItem(@NotNull net.minecraft.world.entity.player.Player player) {
        ItemStack stack = player.getMainHandItem();
        return !stack.isEmpty() && !(stack.getItem() instanceof net.minecraft.world.item.BlockItem);
    }

    @Range(from = 0, to = 8)
    public static int getSelectedSlot(@NotNull net.minecraft.world.entity.player.Player player) {
        return player.getInventory().getSelectedSlot();
    }

    public static void selectSlot(@NotNull net.minecraft.world.entity.player.Player player, int slot) {
        player.getInventory().setSelectedSlot(slot);
    }

    public static void silentSelectSlot(@NotNull LocalPlayer player, int slot) {
        if (player.connection != null)
            NetworkUtility.sendPacket(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(slot));
    }

    public static void swapToOffhand(@NotNull MinecraftWrapper mc, @NotNull LocalPlayer player, int inventorySlot) {
        var _mc = mc.getRaw();
        int containerSlot = inventorySlotToContainerSlot(inventorySlot);
        if (containerSlot == -1) return;
        _mc.gameMode.handleInventoryMouseClick(player.containerMenu.containerId, containerSlot, 0, InventoryUtility.PICKUP, player);
        _mc.gameMode.handleInventoryMouseClick(player.containerMenu.containerId, 45, 0, InventoryUtility.PICKUP, player);
        _mc.gameMode.handleInventoryMouseClick(player.containerMenu.containerId, containerSlot, 0, InventoryUtility.PICKUP, player);
    }

    public static void quickMoveStack(@NotNull MinecraftWrapper mc, @NotNull LocalPlayer player, int inventorySlot) {
        var _mc = mc.getRaw();
        int containerSlot = inventorySlotToContainerSlot(inventorySlot);
        if (containerSlot == -1) return;
        player.containerMenu.quickMoveStack(player, containerSlot);
    }

    public static void clickSlot(@NotNull MinecraftWrapper mc, @NotNull LocalPlayer player, int inventorySlot, int button, @NotNull ClickType type) {
        var _mc = mc.getRaw();
        int containerSlot = inventorySlotToContainerSlot(inventorySlot);
        if (containerSlot == -1) return;
        _mc.gameMode.handleInventoryMouseClick(player.containerMenu.containerId, containerSlot, button, type, player);
    }

    public static void openInventoryScreen(@NotNull LocalPlayer player) {
        MinecraftWrapper.getWrapper().setScreen(new InventoryScreen(player));
    }

    public static void clickChestSlot(@NotNull MinecraftWrapper mc, @NotNull LocalPlayer player, int containerSlot, @NotNull ClickType type) {
        var _mc = mc.getRaw();
        _mc.gameMode.handleInventoryMouseClick(player.containerMenu.containerId, containerSlot, 0, type, player);
    }

    public static void quickMoveSlot(@NotNull MinecraftWrapper mc, int containerId, int slotIndex) {
        var _mc = mc.getRaw();
        _mc.gameMode.handleInventoryMouseClick(containerId, slotIndex, 0, InventoryUtility.QUICK_MOVE, _mc.player);
    }

    public static void swapSlots(@NotNull MinecraftWrapper mc, int containerId, int slotA, int slotB) {
        var _mc = mc.getRaw();
        _mc.gameMode.handleInventoryMouseClick(containerId, slotA, slotB, InventoryUtility.SWAP, _mc.player);
    }

    public static int getItemUseCooldown(@NotNull LocalPlayer player, @NotNull ItemStack stack) {
        return player.getItemInHand(InteractionHand.MAIN_HAND).getItem() == stack.getItem()
            ? (int) player.getCurrentItemAttackStrengthDelay() : 0;
    }

    public static boolean isItemOnCooldown(@NotNull LocalPlayer player, @NotNull ItemStack stack) {
        return player.getCooldowns().isOnCooldown(stack);
    }

    @NotNull
    public static ItemStack getOffhand(@NotNull net.minecraft.world.entity.player.Player player) {
        return player.getOffhandItem();
    }

    public static boolean isHolding(@NotNull net.minecraft.world.entity.player.Player player, @NotNull Item item) {
        return player.getMainHandItem().is(item);
    }

    public static boolean isHolding(@NotNull net.minecraft.world.entity.player.Player player, @NotNull String itemName) {
        return isItem(player.getMainHandItem(), itemName);
    }

    public static boolean isOffhand(@NotNull net.minecraft.world.entity.player.Player player, @NotNull Item item) {
        return player.getOffhandItem().is(item);
    }

    public static boolean isOffhand(@NotNull net.minecraft.world.entity.player.Player player, @NotNull String itemName) {
        return isItem(player.getOffhandItem(), itemName);
    }

    @NotNull
    public static ItemStack getItem(@NotNull net.minecraft.world.entity.player.Player player, int slot) {
        return player.getInventory().getItem(slot);
    }

    public static int getContainerSize(@NotNull net.minecraft.world.entity.player.Player player) {
        return player.getInventory().getContainerSize();
    }

    public static int containerMenuId(@NotNull net.minecraft.world.entity.player.Player player) {
        return player.containerMenu.containerId;
    }

    public static void handleInventoryClick(@NotNull MinecraftWrapper mc, @NotNull LocalPlayer player, int containerSlot, int button, @NotNull ClickType type) {
        var _mc = mc.getRaw();
        _mc.gameMode.handleInventoryMouseClick(player.containerMenu.containerId, containerSlot, button, type, player);
    }

    public static boolean isItemInSlot(@NotNull net.minecraft.world.entity.player.Player player, int slot, @NotNull String itemName) {
        ItemStack stack = getItem(player, slot);
        return !stack.isEmpty() && matchesItemId(stack, itemName);
    }

    public static boolean isItem(@NotNull ItemStack stack, @NotNull String itemName) {
        return !stack.isEmpty() && matchesItemId(stack, itemName);
    }

    public static boolean isAxeItem(@NotNull ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof net.minecraft.world.item.AxeItem;
    }

    public static boolean isShovelItem(@NotNull ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof net.minecraft.world.item.ShovelItem;
    }

    public static boolean isPickaxeItem(@NotNull ItemStack stack) {
        if (stack.isEmpty()) return false;
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return path.endsWith("_pickaxe");
    }

    public static boolean isSwordItem(@NotNull ItemStack stack) {
        if (stack.isEmpty()) return false;
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return path.endsWith("_sword");
    }

    public static boolean isToolItem(@NotNull ItemStack stack) {
        return isAxeItem(stack) || isPickaxeItem(stack) || isShovelItem(stack) || isSwordItem(stack);
    }

    private static boolean matchesItemId(@NotNull ItemStack stack, @NotNull String itemName) {
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getPath().equals(itemName) || id.toString().equals(itemName);
    }

    @Range(from = -1, to = 8)
    public static int findHotbarSlot(@NotNull net.minecraft.world.entity.player.Player player, @NotNull String itemName) {
        for (int i = 0; i < 9; i++) {
            if (isItemInSlot(player, i, itemName)) return i;
        }
        return -1;
    }

    @Range(from = -1, to = 35)
    public static int findSlot(@NotNull net.minecraft.world.entity.player.Player player, @NotNull String itemName) {
        return findSlot(player, itemName, 0, 36);
    }

    @Range(from = -1, to = 35)
    public static int findSlot(@NotNull net.minecraft.world.entity.player.Player player, @NotNull String itemName, int start, int end) {
        for (int i = start; i < end; i++) {
            if (isItemInSlot(player, i, itemName)) return i;
        }
        return -1;
    }

    @NotNull
    public static ItemEnchantments getEnchantments(@NotNull ItemStack stack) {
        return stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
    }

    public static int getEnchantmentLevel(@NotNull ItemStack stack, @NotNull String enchantmentName) {
        ItemEnchantments enchants = getEnchantments(stack);
        for (Holder<Enchantment> holder : enchants.keySet()) {
            if (holder.getRegisteredName().equals(enchantmentName)) {
                return enchants.getLevel(holder);
            }
        }
        return 0;
    }

    public static boolean hasEnchantment(@NotNull ItemStack stack, @NotNull String enchantmentName) {
        return getEnchantmentLevel(stack, enchantmentName) > 0;
    }

    @Nullable
    public static <T> T getComponent(@NotNull ItemStack stack, @NotNull net.minecraft.core.component.DataComponentType<? extends T> type) {
        return stack.get(type);
    }

    public static <T> T getComponentOrDefault(@NotNull ItemStack stack, @NotNull net.minecraft.core.component.DataComponentType<? extends T> type, T defaultValue) {
        return stack.getOrDefault(type, defaultValue);
    }

    public static boolean isTotem(@NotNull ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.TOTEM_OF_UNDYING);
    }

    public static boolean isCrystal(@NotNull ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.END_CRYSTAL);
    }

    public static boolean isAnchor(@NotNull ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.RESPAWN_ANCHOR);
    }

    public static boolean isGlowstone(@NotNull ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.GLOWSTONE);
    }

    public static boolean isGoldenApple(@NotNull ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.GOLDEN_APPLE);
    }

    public static boolean isEnchantedGoldenApple(@NotNull ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.ENCHANTED_GOLDEN_APPLE);
    }

    public static boolean isTrident(@NotNull ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.TRIDENT);
    }

    public static boolean isBow(@NotNull ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.BOW);
    }

    public static boolean isShulkerBox(@NotNull ItemStack stack) {
        if (stack.isEmpty()) return false;
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return path.contains("shulker_box");
    }

    public static boolean isPotion(@NotNull ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.POTION);
    }

    public static boolean isTippedArrow(@NotNull ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.TIPPED_ARROW);
    }

    @Nullable
    public static net.minecraft.world.entity.EquipmentSlot getEquippableSlot(@NotNull ItemStack stack) {
        var equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null ? equippable.slot() : null;
    }

    @Nullable
    public static net.minecraft.world.food.FoodProperties getFoodProperties(@NotNull ItemStack stack) {
        return stack.get(DataComponents.FOOD);
    }

    @Nullable
    public static net.minecraft.world.item.alchemy.PotionContents getPotionContents(@NotNull ItemStack stack) {
        return stack.get(DataComponents.POTION_CONTENTS);
    }

    @Nullable
    public static net.minecraft.world.item.component.WrittenBookContent getWrittenBookContent(@NotNull ItemStack stack) {
        return stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
    }

    public static void setWrittenBookContent(@NotNull ItemStack stack, @NotNull net.minecraft.world.item.component.WrittenBookContent content) {
        stack.set(DataComponents.WRITTEN_BOOK_CONTENT, content);
    }

    @Nullable
    public static net.minecraft.world.item.component.WritableBookContent getWritableBookContent(@NotNull ItemStack stack) {
        return stack.get(DataComponents.WRITABLE_BOOK_CONTENT);
    }

    public static boolean isWrittenBook(@NotNull ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.WRITTEN_BOOK);
    }

    public static boolean isWritableBook(@NotNull ItemStack stack) {
        return !stack.isEmpty() && stack.is(Items.WRITABLE_BOOK);
    }
}
