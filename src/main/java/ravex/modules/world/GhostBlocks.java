package ravex.modules.world;
import ravex.manager.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.resources.Identifier;
import ravex.event.Subscribe;
import ravex.event.network.PacketEvent;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.network.NetworkUtility;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
public class GhostBlocks extends Module {
    public final ModeParameter mode = new ModeParameter("Mode", "Strict", java.util.List.of("Strict", "Smooth"));
    public final NumberParameter range = new NumberParameter("Range", 6.0, 2.0, 12.0, 0.5);
    private final Set<Long> recentlyMined = new HashSet<>();
    private final Map<Long, String> serverBlocks = new HashMap<>();
    private long lastCheckTime = 0;

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getConnection() == null) return;
        long now = System.currentTimeMillis();
        if (now - lastCheckTime < 500) return;
        lastCheckTime = now;
        double r = range.getValue();
        var pPos = mc.player.blockPosition();
        int minX = (int) Math.floor(pPos.getX() - r);
        int maxX = (int) Math.ceil(pPos.getX() + r);
        int minY = (int) Math.max(mc.level.getMinY(), Math.floor(pPos.getY() - r));
        int maxY = (int) Math.min(mc.level.getMaxY(), Math.ceil(pPos.getY() + r));
        int minZ = (int) Math.floor(pPos.getZ() - r);
        int maxZ = (int) Math.ceil(pPos.getZ() + r);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    long packed = BlockUtility.packPos(x, y, z);
                    var pos = BlockUtility.pos(x, y, z);
                    if (BlockUtility.isAir(mc.level, x, y, z)) continue;
                    if (BlockUtility.destroySpeed(mc.level, pos) < 0) continue;
                    if (!isGhostBlock(x, y, z, getBlockId(BlockUtility.getState(mc.level, x, y, z)))) continue;
                    if ("Strict".equals(mode.getValue())) {
                        NetworkUtility.sendStartDestroy(pos, net.minecraft.core.Direction.UP, 0);
                        NetworkUtility.sendStopDestroy(pos, net.minecraft.core.Direction.UP, 0);
                        recentlyMined.remove(packed);
                        BlockUtility.swing(mc);
                    }
                }
            }
        }
    }
    public static void markMined(net.minecraft.core.BlockPos pos) {
        if (ModuleManager.get(GhostBlocks.class).getEnabled()) {
            ModuleManager.get(GhostBlocks.class).recentlyMined.add(pos.asLong());
        }
    }
    @Subscribe
    public void onPacketEvent(PacketEvent event) {
        if (!getEnabled() || !event.isReceive()) return;
        Object packet = event.getPacket();
        if (packet instanceof ClientboundBlockUpdatePacket blockUpdate) {
            BlockPos pos = blockUpdate.getPos();
            onServerBlockUpdate(pos.getX(), pos.getY(), pos.getZ(), getBlockId(blockUpdate.getBlockState()));
        } else if (packet instanceof ClientboundSectionBlocksUpdatePacket sectionUpdate) {
            sectionUpdate.runUpdates((pos, state) -> {
                onServerBlockUpdate(pos.getX(), pos.getY(), pos.getZ(), getBlockId(state));
            });
        }
    }

    public static void onServerBlockUpdate(int x, int y, int z, String blockId) {
        if (!ModuleManager.get(GhostBlocks.class).getEnabled()) return;
        long packed = BlockUtility.packPos(x, y, z);
        ModuleManager.get(GhostBlocks.class).recentlyMined.remove(packed);
        if (blockId != null && !blockId.equals("minecraft:air")) {
            ModuleManager.get(GhostBlocks.class).serverBlocks.put(packed, blockId);
        } else {
            ModuleManager.get(GhostBlocks.class).serverBlocks.remove(packed);
        }
    }
    public static boolean isGhostBlock(int x, int y, int z, String clientBlockId) {
        if (!ModuleManager.get(GhostBlocks.class).getEnabled()) return false;
        long packed = BlockUtility.packPos(x, y, z);
        if (ModuleManager.get(GhostBlocks.class).recentlyMined.contains(packed)) return true;
        String serverBlock = ModuleManager.get(GhostBlocks.class).serverBlocks.get(packed);
        if (serverBlock != null && !serverBlock.equals(clientBlockId)) return true;
        return false;
    }
    public static String getBlockId(net.minecraft.world.level.block.state.BlockState state) {
        Identifier rl = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return rl != null ? rl.toString() : "minecraft:air";
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(GhostBlocks.class);
    }
    public static GhostBlocks itz() {
        return ModuleManager.get(GhostBlocks.class);
    }
}
