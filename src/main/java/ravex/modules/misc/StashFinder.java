package ravex.modules.misc;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;


@Module(name = "StashFinder", category = "Misc")
public class StashFinder {
    @Parameter(name = "Range", min = 16.0, max = 256.0, step = 8.0)
    public double range = 64.0;
    @Parameter(name = "Containers", options = {"Chest", "TrappedChest", "EnderChest", "Barrel", "Shulker", "Hopper", "Dispenser", "Furnace"})
    public List<String> containerTypes = new ArrayList<>(List.of("Chest", "TrappedChest", "Shulker", "Barrel"));
    @Parameter(name = "MinCount", min = 1, max = 10, step = 1)
    public double minCount = 1;
    @Parameter(name = "MinItems", min = 1, max = 54, step = 1)
    public double minItems = 9;
    @Parameter(name = "ChatLog")
    public boolean logToChat = true;

    private final List<StashEntry> stashes = new ArrayList<>();
    private final Set<Long> knownContainers = new HashSet<>();
    private int scanCursor = 0;
    private long lastScan = 0;
    private static net.minecraft.core.BlockPos lastContainerPos;
    private static net.minecraft.world.level.block.state.BlockState lastContainerState;

    // remembers the last container the player interacted with
    public static void rememberContainer(net.minecraft.world.level.block.state.BlockState state, net.minecraft.core.BlockPos pos) {
        lastContainerState = state;
        lastContainerPos = pos;
    }

    public static net.minecraft.core.BlockPos getRememberedPos() {
        return lastContainerPos;
    }

    private boolean matchesType(net.minecraft.world.level.block.Block block) {
        if (block instanceof net.minecraft.world.level.block.TrappedChestBlock)
            return containerTypes.contains("TrappedChest");
        if (block instanceof net.minecraft.world.level.block.ChestBlock)
            return containerTypes.contains("Chest");
        if (block instanceof net.minecraft.world.level.block.EnderChestBlock)
            return containerTypes.contains("EnderChest");
        if (block instanceof net.minecraft.world.level.block.BarrelBlock)
            return containerTypes.contains("Barrel");
        if (block instanceof net.minecraft.world.level.block.ShulkerBoxBlock)
            return containerTypes.contains("Shulker");
        if (block instanceof net.minecraft.world.level.block.HopperBlock)
            return containerTypes.contains("Hopper");
        if (block instanceof net.minecraft.world.level.block.DispenserBlock
            || block instanceof net.minecraft.world.level.block.DropperBlock)
            return containerTypes.contains("Dispenser");
        if (block instanceof net.minecraft.world.level.block.AbstractFurnaceBlock)
            return containerTypes.contains("Furnace");
        return false;
    }

    public void onDisable() {
        knownContainers.clear();
        stashes.clear();
    }

    // scans nearby loaded chunks for containers, nearest chunks first
    public void onTick() {
        if (!Modules.enabled(StashFinder.class)) return;
        var mc = MinecraftWrapper.getWrapper();
        var level = mc.getLevel();
        var player = mc.getPlayer();
        if (level == null || player == null) return;
        long now = System.currentTimeMillis();
        if (now - lastScan < 100L) return;
        lastScan = now;
        int chunkR = Math.max(1, (int) Math.ceil(range / 16.0));
        int pcx = player.chunkPosition().x;
        int pcz = player.chunkPosition().z;
        List<int[]> offsets = new ArrayList<>();
        for (int dx = -chunkR; dx <= chunkR; dx++) {
            for (int dz = -chunkR; dz <= chunkR; dz++) {
                offsets.add(new int[]{dx, dz});
            }
        }
        offsets.sort((a, b) -> Integer.compare(a[0] * a[0] + a[1] * a[1], b[0] * b[0] + b[1] * b[1]));
        for (int i = 0; i < 8; i++) {
            scanCursor++;
            if (scanCursor >= offsets.size()) scanCursor = 0;
            int[] o = offsets.get(scanCursor);
            scanChunk(level, pcx + o[0], pcz + o[1]);
        }
    }

    private void scanChunk(net.minecraft.client.multiplayer.ClientLevel level, int cx, int cz) {
        var chunk = level.getChunkSource().getChunk(cx, cz, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, false);
        if (chunk == null) return;
        var sections = chunk.getSections();
        int minSectionY = chunk.getMinSectionY();
        net.minecraft.core.BlockPos.MutableBlockPos cursor = new net.minecraft.core.BlockPos.MutableBlockPos();
        int originX = cx << 4;
        int originZ = cz << 4;
        for (int si = 0; si < sections.length; si++) {
            var section = sections[si];
            if (section == null || section.hasOnlyAir()) continue;
            // palette check first, skip sections without containers
            if (!section.maybeHas(state -> matchesType(state.getBlock()))) continue;
            int originY = (minSectionY + si) << 4;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        if (!matchesType(section.getBlockState(x, y, z).getBlock())) continue;
                        cursor.set(originX + x, originY + y, originZ + z);
                        noteContainer(cursor);
                    }
                }
            }
        }
    }

    private void noteContainer(net.minecraft.core.BlockPos pos) {
        long key = pos.asLong();
        if (!knownContainers.add(key)) return;
        double rangeSq = range * range;
        int nearby = 1;
        for (long k : knownContainers) {
            if (k == key) continue;
            if (net.minecraft.core.BlockPos.of(k).distSqr(pos) <= rangeSq) nearby++;
        }
        if (nearby < (int) Math.max(1, minCount)) return;
        if (!logToChat) return;
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() != null) {
            mc.getPlayer().displayClientMessage(
                net.minecraft.network.chat.Component.literal(
                    String.format("§6[StashFinder] §f%d %d %d", pos.getX(), pos.getY(), pos.getZ())),
                false);
        }
    }

    public void onContainerOpened(net.minecraft.core.BlockPos pos, List<net.minecraft.world.item.ItemStack> contents) {
        if (!Modules.enabled(StashFinder.class)) return;
        if (lastContainerState != null && !matchesType(lastContainerState.getBlock())) return;
        if (stashes.stream().anyMatch(s -> s.pos.equals(pos))) return;
        int valuableCount = 0;
        int totalItems = 0;
        for (var stack : contents) {
            if (stack.isEmpty()) continue;
            totalItems++;
            if (isValuable(stack)) valuableCount++;
        }
        if (totalItems < (int) Math.max(1, minItems)) return;
        int nearby = 1;
        double rangeSq = range * range;
        for (StashEntry s : stashes) {
            if (s.pos.distSqr(pos) <= rangeSq) nearby++;
        }
        StashEntry entry = new StashEntry(pos, totalItems, valuableCount, System.currentTimeMillis());
        stashes.add(entry);
        if (logToChat) {
            var mc = MinecraftWrapper.getWrapper();
            if (mc.getPlayer() != null) {
                mc.getPlayer().displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                        String.format("§6[StashFinder] §eFound stash at §f%s §e(%d items, %d valuable, %d containers nearby)",
                            pos.toShortString(), totalItems, valuableCount, nearby)),
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
}
