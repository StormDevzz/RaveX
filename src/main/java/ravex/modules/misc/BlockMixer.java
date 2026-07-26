package ravex.modules.misc;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import ravex.parameter.ModeParameter;

import ravex.utility.player.InventoryUtility;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
@ModuleInfo(name = "BlockMixer", category = "Misc")
public class BlockMixer extends ravex.modules.Module {
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
        return ravex.manager.ModuleManager.INSTANCE.getByName("BlockMixer").getEnabled();
    }

    public static BlockMixer itz() {
        return ravex.manager.ModuleManager.delegate(BlockMixer.class);
    }

    public java.util.List<ravex.parameter.Parameter<?>> getParameters() {
        java.util.List<ravex.parameter.Parameter<?>> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (ravex.parameter.Parameter.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    list.add((ravex.parameter.Parameter<?>) field.get(this));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }
}