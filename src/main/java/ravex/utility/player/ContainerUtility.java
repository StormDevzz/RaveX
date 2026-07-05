package ravex.utility.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import java.util.ArrayList;
import java.util.List;

public class ContainerUtility {
    public static boolean isChestLike(AbstractContainerMenu menu) {
        return menu instanceof ChestMenu
            || menu instanceof HopperMenu
            || menu instanceof DispenserMenu
            || menu instanceof ShulkerBoxMenu;
    }

    public static List<Slot> getContainerSlots(AbstractContainerMenu menu) {
        List<Slot> result = new ArrayList<>();
        int containerSize = menu.slots.size() - 36;
        for (int i = 0; i < containerSize && i < menu.slots.size(); i++)
            result.add(menu.slots.get(i));
        return result;
    }

    public static List<Slot> getPlayerSlots(AbstractContainerMenu menu) {
        List<Slot> result = new ArrayList<>();
        int containerSize = Math.max(0, menu.slots.size() - 36);
        for (int i = containerSize; i < containerSize + 36 && i < menu.slots.size(); i++)
            result.add(menu.slots.get(i));
        return result;
    }

    public static boolean hasItems(List<Slot> slots) {
        return slots.stream().anyMatch(Slot::hasItem);
    }

    public static void quickMoveAll(Minecraft mc, LocalPlayer player, List<Slot> slots) {
        for (Slot slot : slots) {
            if (slot.hasItem())
                mc.gameMode.handleInventoryMouseClick(player.containerMenu.containerId, slot.index, 0, ClickType.QUICK_MOVE, player);
        }
    }

    public static void throwAll(Minecraft mc, LocalPlayer player, List<Slot> slots) {
        for (Slot slot : slots) {
            if (slot.hasItem())
                mc.gameMode.handleInventoryMouseClick(player.containerMenu.containerId, slot.index, 1, ClickType.THROW, player);
        }
    }

    public static void fillFromContainer(Minecraft mc, LocalPlayer player, AbstractContainerMenu menu) {
        List<Slot> containerSlots = getContainerSlots(menu);
        List<Slot> playerSlots = getPlayerSlots(menu);
        java.util.Map<Item, Integer> needed = new java.util.HashMap<>();
        for (Slot ps : playerSlots) {
            if (!ps.hasItem()) continue;
            ItemStack stack = ps.getItem();
            int maxStack = stack.getItem().getDefaultMaxStackSize();
            int space = maxStack - stack.getCount();
            if (space > 0) needed.merge(stack.getItem(), space, Integer::sum);
        }
        if (needed.isEmpty()) return;
        for (Slot cs : containerSlots) {
            if (!cs.hasItem()) continue;
            ItemStack chestStack = cs.getItem();
            Item item = chestStack.getItem();
            int want = needed.getOrDefault(item, 0);
            if (want <= 0) continue;
            mc.gameMode.handleInventoryMouseClick(menu.containerId, cs.index, 0, ClickType.QUICK_MOVE, player);
            int remaining = want - chestStack.getCount();
            if (remaining <= 0) needed.remove(item);
            else needed.put(item, remaining);
        }
    }
}
