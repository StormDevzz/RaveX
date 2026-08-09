package ravex.manager;

import net.minecraft.world.item.ItemStack;
import ravex.mcwrapper.MinecraftWrapper;

public class InventoryManager {
    public static final InventoryManager INSTANCE = new InventoryManager();

    private InventoryManager() {}

    public int findItemInHotbar(net.minecraft.world.item.Item item) {
        var player = MinecraftWrapper.getWrapper().getPlayer();
        if (player == null) return -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    public int findItemInInventory(net.minecraft.world.item.Item item) {
        var player = MinecraftWrapper.getWrapper().getPlayer();
        if (player == null) return -1;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }
}
