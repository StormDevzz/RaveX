package ravex.modules.misc;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.block.BlockUtility;
import net.minecraft.client.Minecraft;
import java.util.ArrayList;
import java.util.List;




@ModuleInfo(name = "StashFinder", category = "Misc")
public class StashFinder implements ModuleAccess {
    @Parameter(name = "Range", min = 16.0, max = 256.0, step = 8.0)
    public double range = 64.0;
    @Parameter(name = "Render")
    public boolean render = true;
    @Parameter(name = "ChatLog")
    public boolean logToChat = true;
    private final List<StashEntry> stashes = new ArrayList<>();
    private double lastCheckX, lastCheckY, lastCheckZ;
    private boolean hasChecked = false;

    public void onContainerOpened(net.minecraft.core.BlockPos pos, List<net.minecraft.world.item.ItemStack> contents) {
        if (!ravex.manager.ModuleManager.INSTANCE.getByName("StashFinder").getEnabled()) return;
        if (stashes.stream().anyMatch(s -> s.pos.equals(pos))) return;
        int valuableCount = 0;
        int totalItems = 0;
        for (var stack : contents) {
            if (stack.isEmpty()) continue;
            totalItems++;
            if (isValuable(stack)) valuableCount++;
        }
        if (totalItems < 9) return;
        StashEntry entry = new StashEntry(pos, totalItems, valuableCount, System.currentTimeMillis());
        stashes.add(entry);
        if (logToChat) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                        String.format("§6[StashFinder] §eFound stash at §f%s §e(%d items, %d valuable)",
                            pos.toShortString(), totalItems, valuableCount)),
                    false);
            }
        }
    }
    public List<StashEntry> getStashes() {
        return new ArrayList<>(stashes);
    }
    public void clearStashes() {
        stashes.clear();
    }
    private boolean isValuable(net.minecraft.world.item.ItemStack stack) {
        String name = stack.getItem().getName(stack.getItem().getDefaultInstance()).getString().toLowerCase();
        if (name.contains("diamond") || name.contains("emerald") || name.contains("gold")
            || name.contains("iron") || name.contains("netherite") || name.contains("enchanted")
            || name.contains("beacon") || name.contains("elytra") || name.contains("shulker")
            || name.contains("totem") || name.contains("godApple") || name.contains("notchApple")
            || name.contains("trident") || name.contains("spawner")) return true;
        var enchantments = stack.get(net.minecraft.core.component.DataComponents.ENCHANTMENTS);
        if (enchantments != null && !enchantments.isEmpty()) return true;
        return false;
    }
    public static class StashEntry {
        public final net.minecraft.core.BlockPos pos;
        public final int totalItems;
        public final int valuableItems;
        public final long discoveredAt;
        public StashEntry(net.minecraft.core.BlockPos pos, int totalItems, int valuableItems, long discoveredAt) {
            this.pos = pos;
            this.totalItems = totalItems;
            this.valuableItems = valuableItems;
            this.discoveredAt = discoveredAt;
        }
    }

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("StashFinder").getEnabled();
    }

    public static StashFinder itz() {
        return ravex.manager.ModuleManager.delegate(StashFinder.class);
    }


}