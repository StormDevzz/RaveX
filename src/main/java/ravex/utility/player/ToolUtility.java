package ravex.utility.player;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class ToolUtility {
    public static int findBestToolSlot(LocalPlayer player, BlockState state) {
        int bestSlot = player.getInventory().getSelectedSlot();
        float bestSpeed = player.getInventory().getItem(bestSlot).getDestroySpeed(state);
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            float speed = stack.getDestroySpeed(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }
        return bestSlot != player.getInventory().getSelectedSlot() ? bestSlot : -1;
    }

    public static float getDestroySpeed(ItemStack stack, BlockState state) {
        return stack.isEmpty() ? 0.0f : stack.getDestroySpeed(state);
    }
}
