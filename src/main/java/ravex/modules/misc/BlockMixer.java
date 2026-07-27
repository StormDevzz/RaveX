package ravex.modules.misc;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.player.InventoryUtility;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;




@Module(name = "BlockMixer", category = "Misc")
public class BlockMixer {
    @Parameter(name = "Swap", modes = {"Normal", "Silent"})
    public String swap = "Normal";
    private static final Random RANDOM = new Random();

    public void shuffle() {
        if (!Modules.enabled(BlockMixer.class)) return;
        var mc = MinecraftWrapper.getInstance();
        net.minecraft.client.player.LocalPlayer player = mc.player;
        if (player == null) return;
        List<Integer> blockSlots = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(player, i);
            if (InventoryUtility.isBlockItem(stack)) {
                blockSlots.add(i);
            }
        }
        if (blockSlots.size() < 2) return;
        if ("Normal".equals(swap)) {
            int a = blockSlots.get(RANDOM.nextInt(blockSlots.size()));
            int b = blockSlots.get(RANDOM.nextInt(blockSlots.size()));
            while (b == a && blockSlots.size() > 1) {
                b = blockSlots.get(RANDOM.nextInt(blockSlots.size()));
            }
            if (a == b) return;
            InventoryUtility.swapSlots(ravex.mcwrapper.MinecraftWrapper.getWrapper(), player.containerMenu.containerId, a, b);
        } else {
            int target = blockSlots.get(RANDOM.nextInt(blockSlots.size()));
            InventoryUtility.selectSlot(player, target);
        }
    }






}