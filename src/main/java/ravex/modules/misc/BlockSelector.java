package ravex.modules.misc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.BlockItem;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.utility.player.InventoryUtility;
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
            var stack = InventoryUtility.getItem(player, i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
                blockSlots.add(i);
            }
        }
        if (blockSlots.isEmpty()) return;
        int currentSlot = InventoryUtility.getSelectedSlot(player);
        int targetSlot = blockSlots.get(RANDOM.nextInt(blockSlots.size()));
        if (targetSlot != currentSlot) {
            InventoryUtility.selectSlot(player, targetSlot);
        }
    }
}
