package ravex.modules.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import ravex.mixin.player.AccessorContainerScreen;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;

import java.util.ArrayList;
import java.util.List;

public class ExtraChest extends Module {
    public static final ExtraChest INSTANCE = new ExtraChest();

    public final BooleanParameter steal   = new BooleanParameter("Steal", true);
    public final BooleanParameter dump    = new BooleanParameter("Dump",  true);
    public final BooleanParameter fill    = new BooleanParameter("Fill",  true);
    public final BooleanParameter dropAll = new BooleanParameter("Drop All", true);

    private static final int BTN_W   = 60;
    private static final int BTN_H   = 20;
    private static final int BTN_GAP = 3;

    private ExtraChest() {
        super("ExtraChest", Category.PLAYER);
        addParameter(steal);
        addParameter(dump);
        addParameter(fill);
        addParameter(dropAll);
    }

    public void onRenderButtons(AbstractContainerScreen<?> screen, GuiGraphics graphics, int mouseX, int mouseY) {
        if (!getEnabled()) return;
        if (!isChestLike(screen)) return;

        AccessorContainerScreen acc = (AccessorContainerScreen)(Object)screen;
        List<ButtonDef> buttons = getButtons();
        int startX = acc.getLeftPos() + acc.getImageWidth() + 5;
        int startY = acc.getTopPos();

        for (int i = 0; i < buttons.size(); i++) {
            int by = startY + i * (BTN_H + BTN_GAP);
            drawVanillaButton(graphics, buttons.get(i).label(), startX, by, mouseX, mouseY);
        }
    }

    public boolean onMouseClicked(AbstractContainerScreen<?> screen, int mouseX, int mouseY) {
        if (!getEnabled()) return false;
        if (!isChestLike(screen)) return false;

        AccessorContainerScreen acc = (AccessorContainerScreen)(Object)screen;
        List<ButtonDef> buttons = getButtons();
        int startX = acc.getLeftPos() + acc.getImageWidth() + 5;
        int startY = acc.getTopPos();

        for (int i = 0; i < buttons.size(); i++) {
            int by = startY + i * (BTN_H + BTN_GAP);
            if (mouseX >= startX && mouseX <= startX + BTN_W
             && mouseY >= by && mouseY <= by + BTN_H) {
                handleAction(screen, buttons.get(i).action());
                return true;
            }
        }
        return false;
    }

    private void drawVanillaButton(GuiGraphics graphics, String label, int x, int y, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + BTN_W && mouseY >= y && mouseY <= y + BTN_H;

        int topColor    = hovered ? 0xFFBEBEBE : 0xFFA0A0A0;
        int bottomColor = hovered ? 0xFF6E6E6E : 0xFF505050;
        int bgColor     = hovered ? 0xFF8C8C8C : 0xFF6C6C6C;

        graphics.fill(x, y, x + BTN_W, y + BTN_H, bgColor);
        graphics.fill(x, y, x + BTN_W, y + 1, topColor);
        graphics.fill(x, y, x + 1, y + BTN_H, topColor);
        graphics.fill(x, y + BTN_H - 1, x + BTN_W, y + BTN_H, bottomColor);
        graphics.fill(x + BTN_W - 1, y, x + BTN_W, y + BTN_H, bottomColor);

        int textColor = 0xFFFFFFFF;
        int textW = Minecraft.getInstance().font.width(label);
        int textX = x + (BTN_W - textW) / 2;
        int textY = y + (BTN_H - 8) / 2;
        graphics.drawString(Minecraft.getInstance().font, label, textX, textY, textColor, true);
    }

    private void handleAction(AbstractContainerScreen<?> screen, String action) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        AbstractContainerMenu menu = screen.getMenu();

        switch (action) {
            case "STEAL" -> doSteal(mc, player, menu);
            case "DUMP"  -> doDump(mc, player, menu);
            case "FILL"  -> doFill(mc, player, menu);
            case "DROP"  -> doDropAll(mc, player, menu);
        }
    }

    private void doSteal(Minecraft mc, LocalPlayer player, AbstractContainerMenu menu) {
        for (Slot slot : getContainerSlots(menu)) {
            if (slot.hasItem()) {
                mc.gameMode.handleInventoryMouseClick(menu.containerId, slot.index, 0, ClickType.QUICK_MOVE, player);
            }
        }
    }

    private void doDump(Minecraft mc, LocalPlayer player, AbstractContainerMenu menu) {
        for (Slot slot : getAllPlayerSlots(menu)) {
            if (slot.hasItem()) {
                mc.gameMode.handleInventoryMouseClick(menu.containerId, slot.index, 0, ClickType.QUICK_MOVE, player);
            }
        }
    }

    /**
     * Fill: replenish player's partial stacks from the chest.
     * For each item type the player has, calculate how many more fit into
     * their stacks (respecting max stack size: 64/16/1), then QUICK_MOVE
     * matching items from chest to player.
     */
    private void doFill(Minecraft mc, LocalPlayer player, AbstractContainerMenu menu) {
        List<Slot> chestSlots = getContainerSlots(menu);
        List<Slot> playerSlots = getAllPlayerSlots(menu);

        // Build a map: item type -> how many the player can still hold
        java.util.Map<Item, Integer> needed = new java.util.HashMap<>();
        for (Slot ps : playerSlots) {
            if (!ps.hasItem()) continue;
            ItemStack stack = ps.getItem();
            Item item = stack.getItem();
            int maxStack = item.getDefaultMaxStackSize();
            int space = maxStack - stack.getCount();
            if (space > 0) {
                needed.merge(item, space, Integer::sum);
            }
        }

        if (needed.isEmpty()) return;

        // For each chest slot, try to fill player's partial stacks
        for (Slot cs : chestSlots) {
            if (!cs.hasItem()) continue;
            ItemStack chestStack = cs.getItem();
            Item item = chestStack.getItem();
            int want = needed.getOrDefault(item, 0);
            if (want <= 0) continue;

            mc.gameMode.handleInventoryMouseClick(menu.containerId, cs.index, 0, ClickType.QUICK_MOVE, player);

            // QUICK_MOVE from chest to player merges with existing stacks
            // We can't know exactly how many were moved, so recalculate
            int remaining = want - chestStack.getCount();
            if (remaining <= 0) {
                needed.remove(item);
            } else {
                needed.put(item, remaining);
            }
        }
    }

    /** Drop All: throw all container items on the ground */
    private void doDropAll(Minecraft mc, LocalPlayer player, AbstractContainerMenu menu) {
        for (Slot slot : getContainerSlots(menu)) {
            if (slot.hasItem()) {
                mc.gameMode.handleInventoryMouseClick(menu.containerId, slot.index, 1, ClickType.THROW, player);
            }
        }
    }

    private boolean isChestLike(AbstractContainerScreen<?> screen) {
        AbstractContainerMenu menu = screen.getMenu();
        return menu instanceof ChestMenu
            || menu instanceof HopperMenu
            || menu instanceof DispenserMenu
            || menu instanceof ShulkerBoxMenu;
    }

    private List<Slot> getContainerSlots(AbstractContainerMenu menu) {
        List<Slot> result = new ArrayList<>();
        int containerSize = menu.slots.size() - 36;
        if (containerSize <= 0) return result;
        for (int i = 0; i < containerSize; i++) result.add(menu.slots.get(i));
        return result;
    }

    private List<Slot> getAllPlayerSlots(AbstractContainerMenu menu) {
        List<Slot> result = new ArrayList<>();
        int containerSize = Math.max(0, menu.slots.size() - 36);
        for (int i = containerSize; i < containerSize + 36 && i < menu.slots.size(); i++) {
            result.add(menu.slots.get(i));
        }
        return result;
    }

    private List<ButtonDef> getButtons() {
        List<ButtonDef> list = new ArrayList<>();
        if (steal.getValue()) list.add(new ButtonDef("Steal", "STEAL"));
        if (dump.getValue())  list.add(new ButtonDef("Dump",  "DUMP"));
        if (fill.getValue())  list.add(new ButtonDef("Fill",  "FILL"));
        if (dropAll.getValue()) list.add(new ButtonDef("Drop", "DROP"));
        return list;
    }

    record ButtonDef(String label, String action) {}
}
