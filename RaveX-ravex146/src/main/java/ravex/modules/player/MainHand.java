package ravex.modules.player;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import java.util.List;

public class MainHand extends Module {
    public static final MainHand INSTANCE = new MainHand();

    public final ModeParameter item = new ModeParameter("Item", "Sword", List.of("Sword", "Gapple", "Crystal", "Shield", "Totem"));
    public final NumberParameter minHealth = new NumberParameter("Min HP", 8.0, 1.0, 20.0, 0.5);

    private MainHand() {
        super("MainHand", Category.COMBAT);
        addParameter(item);
        addParameter(minHealth);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null) return;

        Item targetItem = Items.AIR;
        boolean forceTotem = p.getHealth() <= minHealth.getValue();

        if (forceTotem) {
            targetItem = Items.TOTEM_OF_UNDYING;
        } else {
            String choice = item.getValue();
            if (choice.equals("Sword")) {
                
                int swordSlot = findSwordSlot();
                if (swordSlot != -1) {
                    p.getInventory().setSelectedSlot(swordSlot);
                    return;
                }
            } else if (choice.equals("Gapple")) targetItem = Items.GOLDEN_APPLE;
            else if (choice.equals("Crystal")) targetItem = Items.END_CRYSTAL;
            else if (choice.equals("Shield")) targetItem = Items.SHIELD;
            else if (choice.equals("Totem")) targetItem = Items.TOTEM_OF_UNDYING;
        }

        if (targetItem == Items.AIR) return;

        
        if (p.getMainHandItem().is(targetItem) || (targetItem == Items.GOLDEN_APPLE && p.getMainHandItem().is(Items.ENCHANTED_GOLDEN_APPLE))) {
            return;
        }

        
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = p.getInventory().getItem(i);
            if (stack.is(targetItem) || (targetItem == Items.GOLDEN_APPLE && stack.is(Items.ENCHANTED_GOLDEN_APPLE))) {
                slot = i;
                break;
            }
        }

        if (slot != -1) {
            p.getInventory().setSelectedSlot(slot);
        }
    }

    private int findSwordSlot() {
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        for (int i = 0; i < 9; i++) {
            String name = p.getInventory().getItem(i).getItem().toString().toLowerCase();
            if (name.contains("sword")) return i;
        }
        return -1;
    }
}
