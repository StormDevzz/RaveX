package ravex.modules.misc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import ravex.modules.Category;
import ravex.modules.Module;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
public class BlockSelector extends Module {
    public static final BlockSelector INSTANCE = new BlockSelector();
    private static final Random RANDOM = new Random();

    public void selectRandomBlock() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        List<Integer> blockSlots = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
                blockSlots.add(i);
            }
        }
        if (blockSlots.isEmpty()) return;
        int currentSlot = player.getInventory().getSelectedSlot();
        int targetSlot = blockSlots.get(RANDOM.nextInt(blockSlots.size()));
        if (targetSlot != currentSlot) {
            player.getInventory().setSelectedSlot(targetSlot);
        }
    }
}
