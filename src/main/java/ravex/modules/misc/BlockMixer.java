package ravex.modules.misc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.manager.ModuleManager;
import ravex.utility.player.InventoryUtility;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
public class BlockMixer extends Module {
    public final ModeParameter swap = new ModeParameter("Swap", "Normal", List.of("Normal", "Silent"));
    private static final Random RANDOM = new Random();

    public void shuffle() {
        if (!getEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        List<Integer> blockSlots = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(player, i);
            if (InventoryUtility.isBlockItem(stack)) {
                blockSlots.add(i);
            }
        }
        if (blockSlots.size() < 2) return;
        if ("Normal".equals(swap.getValue())) {
            int a = blockSlots.get(RANDOM.nextInt(blockSlots.size()));
            int b = blockSlots.get(RANDOM.nextInt(blockSlots.size()));
            while (b == a && blockSlots.size() > 1) {
                b = blockSlots.get(RANDOM.nextInt(blockSlots.size()));
            }
            if (a == b) return;
            InventoryUtility.swapSlots(mc, player.containerMenu.containerId, a, b);
        } else {
            int target = blockSlots.get(RANDOM.nextInt(blockSlots.size()));
            InventoryUtility.selectSlot(player, target);
        }
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(BlockMixer.class);
    }

    public static BlockMixer itz() {
        return ModuleManager.get(BlockMixer.class);
    }
}
