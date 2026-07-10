package ravex.modules.player;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;

import java.util.List;

public class ElytraSwap extends Module {
    public static final ElytraSwap INSTANCE = new ElytraSwap();

    public final ModeParameter mode = new ModeParameter("Mode", "Positive1", List.of("Positive1", "Positive2", "Positive3"));

    private int state = 0;
    private int targetContainerSlot = -1;
    private long lastActionTime = 0;

    private ElytraSwap() {
        super("ElytraSwap", Category.PLAYER);
        addParameter(mode);
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.gameMode == null) {
            setEnabled(false);
            return;
        }

        ItemStack chestItem = p.getItemBySlot(EquipmentSlot.CHEST);
        boolean hasElytra = chestItem.is(Items.ELYTRA);

        int foundSlot = -1;
        if (hasElytra) {
            foundSlot = findChestplateSlot(p);
        } else {
            foundSlot = findElytraSlot(p);
        }

        if (foundSlot == -1) {
            p.displayClientMessage(net.minecraft.network.chat.Component.literal("§7[§5ElytraSwap§7] §cNo replacement chest item found!"), false);
            setEnabled(false);
            return;
        }

        targetContainerSlot = foundSlot < 9 ? foundSlot + 36 : foundSlot;
        state = 0;
        lastActionTime = System.currentTimeMillis();

        String currentMode = mode.getValue();
        if (currentMode.equals("Positive1")) {

            click(mc, targetContainerSlot, 0, ClickType.PICKUP);
            click(mc, 6, 0, ClickType.PICKUP);
            click(mc, targetContainerSlot, 0, ClickType.PICKUP);
            setEnabled(false);
        } else if (currentMode.equals("Positive3")) {

            mc.setScreen(new InventoryScreen(p));
        }
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.gameMode == null) {
            setEnabled(false);
            return;
        }

        String currentMode = mode.getValue();
        if (currentMode.equals("Positive1")) return;

        long now = System.currentTimeMillis();
        if (now - lastActionTime < 100) return;

        if (state == 0) {
            click(mc, targetContainerSlot, 0, ClickType.PICKUP);
            state = 1;
            lastActionTime = now;
        } else if (state == 1) {
            click(mc, 6, 0, ClickType.PICKUP);
            state = 2;
            lastActionTime = now;
        } else if (state == 2) {
            click(mc, targetContainerSlot, 0, ClickType.PICKUP);
            state = 3;
            lastActionTime = now;
        } else if (state == 3) {
            if (currentMode.equals("Positive3")) {
                mc.setScreen(null);
            }
            setEnabled(false);
        }
    }

    private void click(Minecraft mc, int slot, int button, ClickType type) {
        if (mc.gameMode != null && mc.player != null) {
            mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, slot, button, type, mc.player);
        }
    }

    private int findChestplateSlot(LocalPlayer p) {
        int bestSlot = -1;
        ItemStack bestStack = ItemStack.EMPTY;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = p.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.get(DataComponents.EQUIPPABLE) != null) {
                var equippable = stack.get(DataComponents.EQUIPPABLE);
                if (equippable.slot() == EquipmentSlot.CHEST && !stack.is(Items.ELYTRA)) {
                    if (bestSlot == -1 || isBetterChestplate(stack, bestStack)) {
                        bestSlot = i;
                        bestStack = stack;
                    }
                }
            }
        }
        return bestSlot;
    }

    private boolean isBetterChestplate(ItemStack a, ItemStack b) {
        if (b.isEmpty()) return true;
        int defenseA = 0;
        var modA = a.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modA != null) {
            defenseA = (int) modA.compute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR, 0.0, EquipmentSlot.CHEST);
        }
        int defenseB = 0;
        var modB = b.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modB != null) {
            defenseB = (int) modB.compute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR, 0.0, EquipmentSlot.CHEST);
        }
        return defenseA > defenseB;
    }

    private int findElytraSlot(LocalPlayer p) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = p.getInventory().getItem(i);
            if (stack.is(Items.ELYTRA)) {
                return i;
            }
        }
        return -1;
    }
}
