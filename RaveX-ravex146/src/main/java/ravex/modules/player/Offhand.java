package ravex.modules.player;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.inventory.ClickType;
import java.util.List;

public class Offhand extends Module {
    public static final Offhand INSTANCE = new Offhand();

    public final ModeParameter item = new ModeParameter("Item", "Totem", List.of("Totem", "Gapple", "Crystal", "Shield"));
    public final NumberParameter minHealth = new NumberParameter("Min HP", 8.0, 1.0, 20.0, 0.5);

    private Offhand() {
        super("Offhand", Category.COMBAT);
        addParameter(item);
        addParameter(minHealth);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null || mc.gameMode == null) return;

        
        Item targetItem = Items.TOTEM_OF_UNDYING;
        boolean forceTotem = p.getHealth() <= minHealth.getValue();

        if (!forceTotem) {
            String choice = item.getValue();
            if (choice.equals("Totem")) targetItem = Items.TOTEM_OF_UNDYING;
            else if (choice.equals("Gapple")) targetItem = Items.GOLDEN_APPLE;
            else if (choice.equals("Crystal")) targetItem = Items.END_CRYSTAL;
            else if (choice.equals("Shield")) targetItem = Items.SHIELD;
        }

        
        if (p.getOffhandItem().is(targetItem)) return;

        
        int foundSlot = -1;
        
        for (int i = 0; i < 36; i++) {
            ItemStack stack = p.getInventory().getItem(i);
            if (stack.is(targetItem) || (targetItem == Items.GOLDEN_APPLE && stack.is(Items.ENCHANTED_GOLDEN_APPLE))) {
                foundSlot = i;
                break;
            }
        }

        
        if (foundSlot == -1 && !targetItem.equals(Items.TOTEM_OF_UNDYING)) {
            for (int i = 0; i < 36; i++) {
                if (p.getInventory().getItem(i).is(Items.TOTEM_OF_UNDYING)) {
                    foundSlot = i;
                    break;
                }
            }
        }

        if (foundSlot != -1) {
            swapToOffhand(foundSlot);
        }
    }

    private void swapToOffhand(int invSlot) {
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null || mc.gameMode == null) return;

        int containerSlot = invSlot < 9 ? invSlot + 36 : invSlot;
        mc.gameMode.handleInventoryMouseClick(p.containerMenu.containerId, containerSlot, 0, ClickType.PICKUP, p);
        mc.gameMode.handleInventoryMouseClick(p.containerMenu.containerId, 45, 0, ClickType.PICKUP, p);
        mc.gameMode.handleInventoryMouseClick(p.containerMenu.containerId, containerSlot, 0, ClickType.PICKUP, p);
    }
}
