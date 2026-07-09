package ravex.modules.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;

import java.util.ArrayList;
import java.util.List;

public class StashFinder extends Module {
    public static final StashFinder INSTANCE = new StashFinder();

    public final NumberParameter range = new NumberParameter("Range", 64.0, 16.0, 256.0, 8.0);
    public final BooleanParameter render = new BooleanParameter("Render", true);
    public final BooleanParameter logToChat = new BooleanParameter("Chat Log", true);

    private final List<StashEntry> stashes = new ArrayList<>();
    private double lastCheckX, lastCheckY, lastCheckZ;
    private boolean hasChecked = false;

    private StashFinder() {
        super("StashFinder", Category.MISC);
        addParameter(range);
        addParameter(render);
        addParameter(logToChat);
    }

    
    public void onContainerOpened(BlockPos pos, List<ItemStack> contents) {
        if (!getEnabled()) return;
        if (stashes.stream().anyMatch(s -> s.pos.equals(pos))) return;

        int valuableCount = 0;
        int totalItems = 0;
        for (ItemStack stack : contents) {
            if (stack.isEmpty()) continue;
            totalItems++;
            if (isValuable(stack)) valuableCount++;
        }

        if (totalItems < 9) return; 

        StashEntry entry = new StashEntry(pos, totalItems, valuableCount, System.currentTimeMillis());
        stashes.add(entry);

        if (logToChat.getValue()) {
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

    private boolean isValuable(ItemStack stack) {
        String name = stack.getItem().getName(stack.getItem().getDefaultInstance()).getString().toLowerCase();
        
        if (name.contains("diamond") || name.contains("emerald") || name.contains("gold")
            || name.contains("iron") || name.contains("netherite") || name.contains("enchanted")
            || name.contains("beacon") || name.contains("elytra") || name.contains("shulker")
            || name.contains("totem") || name.contains("god apple") || name.contains("notch apple")
            || name.contains("trident") || name.contains("spawner")) return true;

        
        var enchantments = stack.get(net.minecraft.core.component.DataComponents.ENCHANTMENTS);
        if (enchantments != null && !enchantments.isEmpty()) return true;

        return false;
    }

    public static class StashEntry {
        public final BlockPos pos;
        public final int totalItems;
        public final int valuableItems;
        public final long discoveredAt;

        public StashEntry(BlockPos pos, int totalItems, int valuableItems, long discoveredAt) {
            this.pos = pos;
            this.totalItems = totalItems;
            this.valuableItems = valuableItems;
            this.discoveredAt = discoveredAt;
        }
    }
}
