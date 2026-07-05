package ravex.utility.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.phys.EntityHitResult;
import java.util.function.Predicate;

public class InventoryUtility {
    public static int inventorySlotToContainerSlot(int slot) {
        if (slot < 0 || slot > 35) return -1;
        return slot < 9 ? slot + 36 : slot;
    }

    public static int findSlot(LocalPlayer player, Item target) {
        for (int i = 0; i < 36; i++) {
            if (player.getInventory().getItem(i).is(target)) return i;
        }
        return -1;
    }

    public static int findSlot(LocalPlayer player, Predicate<ItemStack> predicate) {
        for (int i = 0; i < 36; i++) {
            if (predicate.test(player.getInventory().getItem(i))) return i;
        }
        return -1;
    }

    public static int findSlot(LocalPlayer player, Item target, int startInclusive, int endExclusive) {
        for (int i = startInclusive; i < endExclusive; i++) {
            if (player.getInventory().getItem(i).is(target)) return i;
        }
        return -1;
    }

    public static int findSlot(LocalPlayer player, Predicate<ItemStack> predicate, int startInclusive, int endExclusive) {
        for (int i = startInclusive; i < endExclusive; i++) {
            if (predicate.test(player.getInventory().getItem(i))) return i;
        }
        return -1;
    }

    public static int findHotbarSlot(LocalPlayer player, Item target) {
        return findSlot(player, target, 0, 9);
    }

    public static int findEmptyHotbarSlot(LocalPlayer player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i).isEmpty()) return i;
        }
        return -1;
    }

    public static int countItem(LocalPlayer player, Item target) {
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(target)) count += stack.getCount();
        }
        return count;
    }

    public static ItemStack getMainHand(LocalPlayer player) {
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

    public static boolean isHoldingBlock(LocalPlayer player) {
        ItemStack stack = player.getMainHandItem();
        return !stack.isEmpty() && stack.getItem() instanceof net.minecraft.world.item.BlockItem;
    }

    public static boolean isHoldingItem(LocalPlayer player) {
        ItemStack stack = player.getMainHandItem();
        return !stack.isEmpty() && !(stack.getItem() instanceof net.minecraft.world.item.BlockItem);
    }

    public static int getSelectedSlot(LocalPlayer player) {
        return player.getInventory().getSelectedSlot();
    }

    public static void selectSlot(LocalPlayer player, int slot) {
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
}
