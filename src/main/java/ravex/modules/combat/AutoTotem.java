package ravex.modules.combat;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ClickType;
import java.util.List;
import ravex.utility.player.InventoryUtility;
public class AutoTotem extends Module {
    public final ModeParameter offhandItem = new ModeParameter("Offhand", "Totem", List.of("Totem", "Gapple", "Crystal", "Shield", "None"));
    public final ModeParameter mainHandItem = new ModeParameter("MainHand", "Sword", List.of("Sword", "Gapple", "Crystal", "Shield", "Totem", "None"));
    public final NumberParameter minHealth = new NumberParameter("MinHP", 8.0, 1.0, 20.0, 0.5);

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null || mc.gameMode == null) return;
        boolean forceTotem = p.getHealth() <= minHealth.getValue();
        handleOffhand(mc, p, forceTotem);
        handleMainHand(mc, p, forceTotem);
    }
    private void handleOffhand(Minecraft mc, LocalPlayer p, boolean forceTotem) {
        String choice = offhandItem.getValue();
        if (choice.equals("None") && !forceTotem) return;
        net.minecraft.world.item.Item targetItem = net.minecraft.world.item.Items.TOTEM_OF_UNDYING;
        if (!forceTotem) {
            if (choice.equals("Totem")) targetItem = net.minecraft.world.item.Items.TOTEM_OF_UNDYING;
            else if (choice.equals("Gapple")) targetItem = net.minecraft.world.item.Items.GOLDEN_APPLE;
            else if (choice.equals("Crystal")) targetItem = net.minecraft.world.item.Items.END_CRYSTAL;
            else if (choice.equals("Shield")) targetItem = net.minecraft.world.item.Items.SHIELD;
        }
        if (p.getOffhandItem().is(targetItem)) return;
        if (targetItem == net.minecraft.world.item.Items.TOTEM_OF_UNDYING && !p.getOffhandItem().isEmpty() && !InventoryUtility.isTotem(p.getOffhandItem())) return;
        int foundSlot = -1;
        for (int i = 0; i < 36; i++) {
            var stack = InventoryUtility.getItem(p, i);
            if (stack.is(targetItem) || (targetItem == net.minecraft.world.item.Items.GOLDEN_APPLE && InventoryUtility.isEnchantedGoldenApple(stack))) {
                foundSlot = i;
                break;
            }
        }
        if (foundSlot == -1 && !targetItem.equals(net.minecraft.world.item.Items.TOTEM_OF_UNDYING)) {
            for (int i = 0; i < 36; i++) {
                if (InventoryUtility.isTotem(InventoryUtility.getItem(p, i))) {
                    foundSlot = i;
                    break;
                }
            }
        }
        if (foundSlot != -1) {
            swapToOffhand(mc, p, foundSlot);
        }
    }
    private void handleMainHand(Minecraft mc, LocalPlayer p, boolean forceTotem) {
        String mainChoice = mainHandItem.getValue();
        if (mainChoice.equals("None")) return;
        net.minecraft.world.item.Item targetItem = null;
        if (forceTotem) {
            targetItem = net.minecraft.world.item.Items.TOTEM_OF_UNDYING;
        } else {
            if (mainChoice.equals("Sword")) {
                int swordSlot = findSwordSlot();
                if (swordSlot != -1) {
                    InventoryUtility.selectSlot(p, swordSlot);
                    return;
                }
            } else if (mainChoice.equals("Gapple")) targetItem = net.minecraft.world.item.Items.GOLDEN_APPLE;
            else if (mainChoice.equals("Crystal")) targetItem = net.minecraft.world.item.Items.END_CRYSTAL;
            else if (mainChoice.equals("Shield")) targetItem = net.minecraft.world.item.Items.SHIELD;
            else if (mainChoice.equals("Totem")) targetItem = net.minecraft.world.item.Items.TOTEM_OF_UNDYING;
        }
        if (targetItem == null) return;
        if (p.getMainHandItem().is(targetItem) || (targetItem == net.minecraft.world.item.Items.GOLDEN_APPLE && InventoryUtility.isEnchantedGoldenApple(p.getMainHandItem()))) {
            return;
        }
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(p, i);
            if (stack.is(targetItem) || (targetItem == net.minecraft.world.item.Items.GOLDEN_APPLE && InventoryUtility.isEnchantedGoldenApple(stack))) {
                slot = i;
                break;
            }
        }
        if (slot != -1) {
            InventoryUtility.selectSlot(p, slot);
        }
    }
    private void swapToOffhand(Minecraft mc, LocalPlayer p, int invSlot) {
        int containerSlot = invSlot < 9 ? invSlot + 36 : invSlot;
        mc.gameMode.handleInventoryMouseClick(p.containerMenu.containerId, containerSlot, 0, ClickType.PICKUP, p);
        mc.gameMode.handleInventoryMouseClick(p.containerMenu.containerId, 45, 0, ClickType.PICKUP, p);
        mc.gameMode.handleInventoryMouseClick(p.containerMenu.containerId, containerSlot, 0, ClickType.PICKUP, p);
    }
    private int findSwordSlot() {
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        for (int i = 0; i < 9; i++) {
            String name = InventoryUtility.getItem(p, i).getItem().toString().toLowerCase();
            if (name.contains("sword")) return i;
        }
        return -1;
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(AutoTotem.class);
    }
    public static AutoTotem itz() {
        return ModuleManager.get(AutoTotem.class);
    }

}