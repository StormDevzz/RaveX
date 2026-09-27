package ravex.utility.player;

import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import java.util.ArrayList;
import java.util.List;
import ravex.utility.player.InventoryUtility;
import ravex.utility.render.ColorUtility;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DUtility;

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

    public static void quickMoveAll(MinecraftWrapper mc, LocalPlayer player, List<Slot> slots) {
        var _mc = mc.getRaw();
        for (Slot slot : slots) {
            if (slot.hasItem())
                _mc.gameMode.handleInventoryMouseClick(player.containerMenu.containerId, slot.index, 0, InventoryUtility.QUICK_MOVE, player);
        }
    }

    public static void throwAll(MinecraftWrapper mc, LocalPlayer player, List<Slot> slots) {
        var _mc = mc.getRaw();
        for (Slot slot : slots) {
            if (slot.hasItem())
                _mc.gameMode.handleInventoryMouseClick(player.containerMenu.containerId, slot.index, 1, ClickType.THROW, player);
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
        Render2DUtility.drawBorder(graphics, x, y, CHEST_BTN_W, CHEST_BTN_H, 1, botCol);
        graphics.fill(x, y, x + 1, y + CHEST_BTN_H - 1, topCol);
        graphics.fill(x, y, x + CHEST_BTN_W - 1, y + 1, topCol);
        var font = MinecraftWrapper.getWrapper().getFont();
        int tw = font.width(label);
        FontRenderUtility.drawString(graphics, label, x + (CHEST_BTN_W - tw) / 2, y + (CHEST_BTN_H - 8) / 2, 0xFFFFFFFF, true);
    }

    public static boolean isMouseOverButton(int mouseX, int mouseY, int x, int y) {
        return mouseX >= x && mouseX <= x + CHEST_BTN_W && mouseY >= y && mouseY <= y + CHEST_BTN_H;
    }

    public static int buttonBaseColor(String action) {
        return switch (action) {
            case "STEAL" -> 0xFF2E9E5B;
            case "DUMP" -> 0xFFE08A2D;
            case "FILL" -> 0xFF3D8BFD;
            case "DROP" -> 0xFFE05252;
            default -> 0xFF6C6C6C;
        };
    }

    private static int shadeButtonColor(int argb, float factor) {
        int a = (argb >>> 24) & 0xFF;
        int r = Math.min(255, (int) (((argb >>> 16) & 0xFF) * factor));
        int g = Math.min(255, (int) (((argb >>> 8) & 0xFF) * factor));
        int b = Math.min(255, (int) ((argb & 0xFF) * factor));
        return ColorUtility.setAlpha((r << 16) | (g << 8) | b, a);
    }
    public static void drawChestButtonNew(GuiGraphics graphics, String label, String action, int x, int y, boolean hovered, boolean pressed) {
        int base = buttonBaseColor(action);
        int bgAlpha = pressed ? 255 : hovered ? 225 : 160;
        int bg = ColorUtility.withAlpha(base, bgAlpha);
        if (hovered || pressed)
            Render2DUtility.drawGaussianShadow(graphics, x - 2, y - 2, CHEST_BTN_W + 4, CHEST_BTN_H + 4, 8, ColorUtility.withAlpha(base, pressed ? 200 : 120));
        Render2DUtility.drawRound(graphics, x, y, CHEST_BTN_W, CHEST_BTN_H, 4, bg);
        Render2DUtility.drawRound(graphics, x, y, CHEST_BTN_W, 7, 4, ColorUtility.withAlpha(0xFFFFFFFF, pressed ? 20 : 45));
        Render2DUtility.drawRoundBorder(graphics, x, y, CHEST_BTN_W, CHEST_BTN_H, 4, 1, ColorUtility.withAlpha(base, pressed ? 255 : 130));
        var font = MinecraftWrapper.getWrapper().getFont();
        int tw = font.width(label);
        int labelCol = pressed ? 0xFFE8E8E8 : 0xFFFFFFFF;
        FontRenderUtility.drawString(graphics, label, x + (CHEST_BTN_W - tw) / 2, y + (CHEST_BTN_H - 8) / 2 + (pressed ? 1 : 0), labelCol, true);
    }

    public static void drawChestButtonCustom(GuiGraphics graphics, String label, int color, int x, int y, boolean hovered, boolean pressed) {
        int bg = pressed ? shadeButtonColor(color, 0.7f) : hovered ? shadeButtonColor(color, 1.18f) : color;
        int edge = shadeButtonColor(color, pressed ? 0.5f : 0.65f);
        graphics.fill(x, y, x + CHEST_BTN_W, y + CHEST_BTN_H, bg);
        Render2DUtility.drawBorder(graphics, x, y, CHEST_BTN_W, CHEST_BTN_H, 1, edge);
        graphics.fill(x + 1, y, x + CHEST_BTN_W - 1, y + 1, shadeButtonColor(color, hovered && !pressed ? 1.35f : 1.0f));
        var font = MinecraftWrapper.getWrapper().getFont();
        int tw = font.width(label);
        FontRenderUtility.drawString(graphics, label, x + (CHEST_BTN_W - tw) / 2, y + (CHEST_BTN_H - 8) / 2 + (pressed ? 1 : 0), 0xFFFFFFFF, true);
    }

    public static void fillFromContainer(MinecraftWrapper mc, LocalPlayer player, AbstractContainerMenu menu) {
        var _mc = mc.getRaw();
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
            _mc.gameMode.handleInventoryMouseClick(menu.containerId, cs.index, 0, InventoryUtility.QUICK_MOVE, player);
            int remaining = want - chestStack.getCount();
            if (remaining <= 0) needed.remove(item);
            else needed.put(item, remaining);
        }
    }
}
