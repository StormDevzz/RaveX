package ravex.modules.world;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.block.BlockUtility;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.resources.Identifier;
import ravex.event.Subscribe;
import ravex.event.network.PacketEvent;

import ravex.utility.network.NetworkUtility;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
@Module(name = "GhostBlocks", category = "World")
public class GhostBlocks {
    @Parameter(name = "Mode", modes = {"Strict", "Smooth"})
    public String mode = "Strict";
    @Parameter(name = "Range", min = 2.0, max = 12.0, step = 0.5)
    public double range = 6.0;
    private final Set<Long> recentlyMined = new HashSet<>();
    private final Map<Long, String> serverBlocks = new HashMap<>();
    private long lastCheckTime = 0;
    public void onTick() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null || mc.level == null || mc.getConnection() == null) return;
        long now = System.currentTimeMillis();
        if (now - lastCheckTime < 500) return;
        lastCheckTime = now;
        double r = range;
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
                    if ("Strict".equals(mode)) {
                        NetworkUtility.sendStartDestroy(pos, net.minecraft.core.Direction.UP, 0);
                        NetworkUtility.sendStopDestroy(pos, net.minecraft.core.Direction.UP, 0);
                        recentlyMined.remove(packed);
                        BlockUtility.swing(ravex.mcwrapper.MinecraftWrapper.getWrapper());
                    }
                }
            }
        }
    }
    public static void markMined(net.minecraft.core.BlockPos pos) {
        if (Modules.enabled(GhostBlocks.class)) {
            Modules.get(GhostBlocks.class).recentlyMined.add(pos.asLong());
        }
    }
    @Subscribe
    public void onPacketEvent(PacketEvent event) {
        if (!Modules.enabled(GhostBlocks.class) || !event.isReceive()) return;
        Object packet = event.getPacket();
        if (packet instanceof ClientboundBlockUpdatePacket blockUpdate) {
            net.minecraft.core.BlockPos pos = blockUpdate.getPos();
            onServerBlockUpdate(pos.getX(), pos.getY(), pos.getZ(), getBlockId(blockUpdate.getBlockState()));
        } else if (packet instanceof ClientboundSectionBlocksUpdatePacket sectionUpdate) {
            sectionUpdate.runUpdates((pos, state) -> {
                onServerBlockUpdate(pos.getX(), pos.getY(), pos.getZ(), getBlockId(state));
            });
        }
    }

    public static void onServerBlockUpdate(int x, int y, int z, String blockId) {
        if (!Modules.enabled(GhostBlocks.class)) return;
        long packed = BlockUtility.packPos(x, y, z);
        Modules.get(GhostBlocks.class).recentlyMined.remove(packed);
        if (blockId != null && !blockId.equals("minecraft:air")) {
            Modules.get(GhostBlocks.class).serverBlocks.put(packed, blockId);
        } else {
            Modules.get(GhostBlocks.class).serverBlocks.remove(packed);
        }
    }
    public static boolean isGhostBlock(int x, int y, int z, String clientBlockId) {
        if (!Modules.enabled(GhostBlocks.class)) return false;
        long packed = BlockUtility.packPos(x, y, z);
        if (Modules.get(GhostBlocks.class).recentlyMined.contains(packed)) return true;
        String serverBlock = Modules.get(GhostBlocks.class).serverBlocks.get(packed);
        if (serverBlock != null && !serverBlock.equals(clientBlockId)) return true;
        return false;
    }
    public static String getBlockId(net.minecraft.world.level.block.state.BlockState state) {
        Identifier rl = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return rl != null ? rl.toString() : "minecraft:air";
    }





}