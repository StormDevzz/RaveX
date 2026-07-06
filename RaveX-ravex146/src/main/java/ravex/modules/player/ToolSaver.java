package ravex.modules.player;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ToolSaver extends Module {
    public static final ToolSaver INSTANCE = new ToolSaver();

    public final NumberParameter threshold = new NumberParameter("Min Durability", 10.0, 1.0, 50.0, 1.0);

    private ToolSaver() {
        super("ToolSaver", Category.PLAYER);
        addParameter(threshold);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ItemStack mainHand = mc.player.getMainHandItem();
        if (shouldSave(mainHand)) {
            int safeSlot = findSafeHotbarSlot(mc.player);
            if (safeSlot != -1) {
                mc.player.getInventory().setSelectedSlot(safeSlot);
            }
        }
    }

    public boolean shouldSave(ItemStack stack) {
        if (!getEnabled()) return false;
        if (stack.isEmpty() || !stack.isDamageableItem()) return false;
        int maxDmg = stack.getMaxDamage();
        int currentDmg = stack.getDamageValue();
        int durability = maxDmg - currentDmg;
        return durability <= threshold.getValue().intValue();
    }

    private int findSafeHotbarSlot(Player player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!shouldSave(stack)) {
                return i;
            }
        }
        return -1;
    }
}
