package ravex.utility.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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

    public static int getButtonStartX(AbstractContainerScreen<?> screen) {
        var acc = (ravex.mixin.player.AccessorContainerScreen) screen;
        return acc.getLeftPos() + acc.getImageWidth() + 5;
    }

    public static int getButtonStartY(AbstractContainerScreen<?> screen) {
        var acc = (ravex.mixin.player.AccessorContainerScreen) screen;
        return acc.getTopPos();
    }

    public static final int CHEST_BTN_W = 60, CHEST_BTN_H = 20, CHEST_BTN_GAP = 3;

    public static void drawChestButton(GuiGraphics graphics, String label, int x, int y, boolean hovered) {
        int topCol = hovered ? 0xFFBEBEBE : 0xFFA0A0A0, botCol = hovered ? 0xFF6E6E6E : 0xFF505050, bgCol = hovered ? 0xFF8C8C8C : 0xFF6C6C6C;
        graphics.fill(x, y, x + CHEST_BTN_W, y + CHEST_BTN_H, bgCol);
        graphics.fill(x, y, x + CHEST_BTN_W, y + 1, topCol);
        graphics.fill(x, y, x + 1, y + CHEST_BTN_H, topCol);
        graphics.fill(x, y + CHEST_BTN_H - 1, x + CHEST_BTN_W, y + CHEST_BTN_H, botCol);
        graphics.fill(x + CHEST_BTN_W - 1, y, x + CHEST_BTN_W, y + CHEST_BTN_H, botCol);
        var font = Minecraft.getInstance().font;
        int tw = font.width(label);
        graphics.drawString(font, label, x + (CHEST_BTN_W - tw) / 2, y + (CHEST_BTN_H - 8) / 2, 0xFFFFFFFF, true);
    }

    public static boolean isMouseOverButton(int mouseX, int mouseY, int x, int y) {
        return mouseX >= x && mouseX <= x + CHEST_BTN_W && mouseY >= y && mouseY <= y + CHEST_BTN_H;
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
