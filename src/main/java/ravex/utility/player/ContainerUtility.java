package ravex.utility.player;

import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import ravex.utility.misc.ScreenUtility;
import ravex.utility.misc.block.BlockUtility;
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

    // ----- furnace / smoker / brewing stand helpers -----

    private static final int FURNACE_MENU_SLOTS = 39;   // 3 furnace slots + 36 player slots
    private static final int BREWING_MENU_SLOTS = 41;   // 5 brewing slots + 36 player slots

    private static BlockPos smeltTarget = null;
    private static int brewTargetX, brewTargetY, brewTargetZ;
    private static boolean hasBrewTarget = false;

    @Nullable
    private static AbstractContainerMenu openMenu() {
        var player = MinecraftWrapper.getWrapper().getPlayer();
        return player != null ? player.containerMenu : null;
    }

    // true while a furnace, smoker or blast furnace screen is open
    public static boolean isFurnaceScreenOpen() {
        String screen = ScreenUtility.getScreenName(MinecraftWrapper.getWrapper()).toLowerCase();
        return screen.contains("furnace") || screen.contains("smoker");
    }

    // true while a brewing stand screen is open
    public static boolean isBrewingStandOpen() {
        return ScreenUtility.getScreenName(MinecraftWrapper.getWrapper()).toLowerCase().contains("brewingstand");
    }

    // true while a furnace-like menu with 39 slots is open
    public static boolean isFurnaceMenuOpen() {
        AbstractContainerMenu menu = openMenu();
        return menu != null && menu.slots.size() == FURNACE_MENU_SLOTS;
    }

    // true while a brewing stand menu with 41 slots is open
    public static boolean isBrewingMenuOpen() {
        AbstractContainerMenu menu = openMenu();
        return menu != null && menu.slots.size() == BREWING_MENU_SLOTS;
    }

    // remembers the furnace block the crosshair is on, until it is cleared
    public static void rememberSmeltTarget() {
        if (smeltTarget != null) return;
        var mc = MinecraftWrapper.getWrapper();
        var level = mc.getLevel();
        if (level == null || !(mc.getHitResult() instanceof BlockHitResult hit)) return;
        BlockPos pos = hit.getBlockPos();
        if (isFurnaceBlock(BlockUtility.getState(level, pos))) smeltTarget = pos;
    }

    // remembers the brewing stand block the crosshair is on, until it is cleared
    public static void rememberBrewTarget() {
        if (hasBrewTarget) return;
        var mc = MinecraftWrapper.getWrapper();
        var level = mc.getLevel();
        if (level == null || !(mc.getHitResult() instanceof BlockHitResult hit)) return;
        BlockPos pos = hit.getBlockPos();
        if (!BlockUtility.isBlock(BlockUtility.getState(level, pos), "brewing_stand")) return;
        brewTargetX = pos.getX();
        brewTargetY = pos.getY();
        brewTargetZ = pos.getZ();
        hasBrewTarget = true;
    }

    // last furnace/smoker that had its screen open, null when unknown
    @Nullable
    public static BlockPos getSmeltTarget() {
        return smeltTarget;
    }

    public static void clearSmeltTarget() {
        smeltTarget = null;
    }

    // last brewing stand that had its screen open, null when unknown
    @Nullable
    public static BlockPos getBrewTarget() {
        return hasBrewTarget ? BlockUtility.pos(brewTargetX, brewTargetY, brewTargetZ) : null;
    }

    public static void clearBrewTarget() {
        hasBrewTarget = false;
    }

    // vanilla burn progress: burned ticks / total ticks
    public static float furnaceBurnProgress() {
        AbstractContainerMenu menu = openMenu();
        if (menu == null) return 0.0f;
        int burned = menuData(menu, 2);
        int total = menuData(menu, 3);
        if (burned == 0 || total == 0) return 0.0f;
        return Math.min(1.0f, (float) burned / (float) total);
    }

    // synced brew ticks of the open brewing stand menu, 0 when idle
    public static int brewTicks() {
        AbstractContainerMenu menu = openMenu();
        return menu != null ? menuData(menu, 0) : 0;
    }

    // takes the furnace result slot (2), true when a stack was moved
    public static boolean takeFurnaceOutput() {
        var mc = MinecraftWrapper.getWrapper();
        AbstractContainerMenu menu = openMenu();
        if (menu == null || !menu.getSlot(2).hasItem()) return false;
        InventoryUtility.quickMoveSlot(mc, menu.containerId, 2);
        return true;
    }

    // moves a recipe-accepted smelt input into the empty input slot (0)
    public static boolean putSmeltInput() {
        var mc = MinecraftWrapper.getWrapper();
        AbstractContainerMenu menu = openMenu();
        var level = mc.getLevel();
        if (menu == null || level == null || menu.getSlot(0).hasItem()) return false;
        String screen = ScreenUtility.getScreenName(mc).toLowerCase();
        var accepted = level.recipeAccess().propertySet(
            screen.contains("blast") ? RecipePropertySet.BLAST_FURNACE_INPUT
                : screen.contains("smoker") ? RecipePropertySet.SMOKER_INPUT
                : RecipePropertySet.FURNACE_INPUT);
        for (Slot slot : getPlayerSlots(menu)) {
            var stack = slot.getItem();
            if (stack.isEmpty() || !accepted.test(stack)) continue;
            InventoryUtility.quickMoveSlot(mc, menu.containerId, slot.index);
            return true;
        }
        int hotbarStart = 3 + 27;
        for (int i = hotbarStart; i < menu.slots.size(); i++) {
            var stack = menu.getSlot(i).getItem();
            if (stack.isEmpty() || !accepted.test(stack)) continue;
            InventoryUtility.quickMoveSlot(mc, menu.containerId, i);
            return true;
        }
        return false;
    }

    // moves a fuel stack into the empty furnace fuel slot (1)
    public static boolean putFurnaceFuel() {
        var mc = MinecraftWrapper.getWrapper();
        AbstractContainerMenu menu = openMenu();
        var level = mc.getLevel();
        if (menu == null || level == null || menu.getSlot(1).hasItem()) return false;
        for (Slot slot : getPlayerSlots(menu)) {
            var stack = slot.getItem();
            if (stack.isEmpty()) continue;
            if (level.fuelValues().isFuel(stack)) {
                InventoryUtility.quickMoveSlot(mc, menu.containerId, slot.index);
                return true;
            }
        }
        return false;
    }

    // moves blaze powder into the fuel slot when the stand is empty
    public static boolean putBrewingFuel() {
        var mc = MinecraftWrapper.getWrapper();
        AbstractContainerMenu menu = openMenu();
        if (menu == null || menuData(menu, 1) > 0 || menu.getSlot(4).hasItem()) return false;
        for (Slot slot : getPlayerSlots(menu)) {
            if (InventoryUtility.isItem(slot.getItem(), "blaze_powder")) {
                InventoryUtility.quickMoveSlot(mc, menu.containerId, slot.index);
                return true;
            }
        }
        return false;
    }

    // takes the finished potions, true when a stack moved
    public static boolean takeFinishedPotions() {
        var mc = MinecraftWrapper.getWrapper();
        AbstractContainerMenu menu = openMenu();
        if (menu == null) return false;
        for (int slot = 0; slot <= 2; slot++) {
            var stack = menu.getSlot(slot).getItem();
            if (!stack.isEmpty() && !InventoryUtility.isItem(stack, "glass_bottle")) {
                InventoryUtility.quickMoveSlot(mc, menu.containerId, slot);
                return true;
            }
        }
        return false;
    }

    // moves a brewing ingredient into the empty ingredient slot (3)
    public static boolean putBrewIngredient() {
        var mc = MinecraftWrapper.getWrapper();
        AbstractContainerMenu menu = openMenu();
        if (menu == null || menu.getSlot(3).hasItem()) return false;
        for (Slot slot : getPlayerSlots(menu)) {
            var stack = slot.getItem();
            if (stack.isEmpty() || !isBrewIngredient(stack)) continue;
            InventoryUtility.quickMoveSlot(mc, menu.containerId, slot.index);
            return true;
        }
        int hotbarStart = 5 + 27;
        for (int i = hotbarStart; i < menu.slots.size(); i++) {
            var stack = menu.getSlot(i).getItem();
            if (stack.isEmpty() || !isBrewIngredient(stack)) continue;
            InventoryUtility.quickMoveSlot(mc, menu.containerId, i);
            return true;
        }
        return false;
    }

    // moves a glass bottle into the first empty bottle slot (0..2)
    public static boolean putGlassBottle() {
        var mc = MinecraftWrapper.getWrapper();
        AbstractContainerMenu menu = openMenu();
        if (menu == null) return false;
        for (int slot = 0; slot <= 2; slot++) {
            if (menu.getSlot(slot).hasItem()) continue;
            for (Slot invSlot : getPlayerSlots(menu)) {
                if (InventoryUtility.isItem(invSlot.getItem(), "glass_bottle")) {
                    InventoryUtility.quickMoveSlot(mc, menu.containerId, invSlot.index);
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isBrewIngredient(ItemStack stack) {
        return InventoryUtility.isItem(stack, "nether_wart") || InventoryUtility.isItem(stack, "glowstone_dust")
            || InventoryUtility.isItem(stack, "redstone") || InventoryUtility.isItem(stack, "gunpowder")
            || InventoryUtility.isItem(stack, "fermented_spider_eye") || InventoryUtility.isItem(stack, "blaze_powder")
            || InventoryUtility.isItem(stack, "ghast_tear") || InventoryUtility.isItem(stack, "magma_cream")
            || InventoryUtility.isItem(stack, "sugar") || InventoryUtility.isItem(stack, "rabbit_foot")
            || InventoryUtility.isItem(stack, "glistering_melon_slice") || InventoryUtility.isItem(stack, "golden_carrot")
            || InventoryUtility.isItem(stack, "spider_eye") || InventoryUtility.isItem(stack, "pufferfish")
            || InventoryUtility.isItem(stack, "phantom_membrane") || InventoryUtility.isItem(stack, "dragon_breath")
            || InventoryUtility.isItem(stack, "turtle_helmet");
    }

    private static boolean isFurnaceBlock(BlockState state) {
        return BlockUtility.isBlock(state, "furnace")
            || BlockUtility.isBlock(state, "blast_furnace")
            || BlockUtility.isBlock(state, "smoker");
    }

    // progress is only in private synced menu data
    private static int menuData(AbstractContainerMenu menu, int index) {
        for (Class<?> type = menu.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!ContainerData.class.isAssignableFrom(field.getType())) continue;
                try {
                    field.setAccessible(true);
                    ContainerData data = (ContainerData) field.get(menu);
                    return data != null ? data.get(index) : 0;
                } catch (Exception ignored) {
                    return 0;
                }
            }
        }
        return 0;
    }
}
