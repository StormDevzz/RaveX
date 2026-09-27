package ravex.modules.world;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.block.BlockUtility;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
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
    @Parameter(name = "Break")
    public boolean breaking = true;
    @Parameter(name = "Place")
    public boolean placing = true;
    @Parameter(name = "Range", min = 2.0, max = 12.0, step = 0.5)
    public double range = 6.0;
    private final Set<Long> recentlyMined = new HashSet<>();
    private final Set<Long> recentlyPlaced = new HashSet<>();
    private final Map<Long, Long> pendingTime = new HashMap<>();
    private final Map<Long, String> serverBlocks = new HashMap<>();
    private long lastCheckTime = 0;
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null || mc.getLevel() == null || mc.getConnection() == null) return;
        long now = System.currentTimeMillis();
        pendingTime.entrySet().removeIf(e -> now - e.getValue() > 5000L);
        recentlyMined.removeIf(packed -> {
            Long t = pendingTime.get(packed);
            return t != null && now - t > 5000L;
        });
        recentlyPlaced.removeIf(packed -> {
            Long t = pendingTime.get(packed);
            return t != null && now - t > 5000L;
        });
        if (now - lastCheckTime < 500) return;
        lastCheckTime = now;
        double r = range;
        var pPos = player.blockPosition();
        var level = mc.getLevel();
        int minX = (int) Math.floor(pPos.getX() - r);
        int maxX = (int) Math.ceil(pPos.getX() + r);
        int minY = (int) Math.max(level.getMinY(), Math.floor(pPos.getY() - r));
        int maxY = (int) Math.min(level.getMaxY(), Math.ceil(pPos.getY() + r));
        int minZ = (int) Math.floor(pPos.getZ() - r);
        int maxZ = (int) Math.ceil(pPos.getZ() + r);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    long packed = BlockUtility.packPos(x, y, z);
                    var pos = BlockUtility.pos(x, y, z);
                    String clientId = getBlockId(BlockUtility.getState(level, x, y, z));
                    boolean clientAir = "minecraft:air".equals(clientId) || BlockUtility.isAir(level, x, y, z);
                    String serverId = serverBlocks.get(packed);
                    if (breaking && !clientAir && BlockUtility.destroySpeed(level, pos) >= 0 && isBreakGhost(packed, clientId, serverId)) {
                        NetworkUtility.sendStartDestroy(pos, net.minecraft.core.Direction.UP, 0);
                        NetworkUtility.sendStopDestroy(pos, net.minecraft.core.Direction.UP, 0);
                        BlockUtility.swing(mc);
                    } else if (placing && clientAir && isPlaceGhost(packed, serverId)) {
                        recentlyMined.remove(packed);
                        recentlyPlaced.remove(packed);
                        serverBlocks.remove(packed);
                    }
                }
            }
        }
    }
    private boolean isBreakGhost(long packed, String clientId, String serverId) {
        if (recentlyMined.contains(packed)) return true;
        if (recentlyPlaced.contains(packed)) return false;
        return serverId != null && !serverId.equals(clientId) && !"minecraft:air".equals(serverId);
    }
    private boolean isPlaceGhost(long packed, String serverId) {
        if (recentlyPlaced.contains(packed)) return true;
        if (recentlyMined.contains(packed)) return false;
        return serverId != null && "minecraft:air".equals(serverId);
    }
    public static void markMined(net.minecraft.core.BlockPos pos) {
        if (Modules.enabled(GhostBlocks.class)) {
            GhostBlocks m = Modules.get(GhostBlocks.class);
            long packed = pos.asLong();
            m.recentlyMined.add(packed);
            m.recentlyPlaced.remove(packed);
            m.pendingTime.put(packed, System.currentTimeMillis());
        }
    }
    public static void markPlaced(net.minecraft.core.BlockPos pos) {
        if (Modules.enabled(GhostBlocks.class)) {
            GhostBlocks m = Modules.get(GhostBlocks.class);
            long packed = pos.asLong();
            m.recentlyPlaced.add(packed);
            m.recentlyMined.remove(packed);
            m.pendingTime.put(packed, System.currentTimeMillis());
        }
    }
    @Subscribe
    public void onPacketEvent(PacketEvent event) {
        if (!Modules.enabled(GhostBlocks.class)) return;
        Object packet = event.getPacket();
        if (event.isReceive()) {
            if (packet instanceof ClientboundBlockUpdatePacket blockUpdate) {
                net.minecraft.core.BlockPos pos = blockUpdate.getPos();
                onServerBlockUpdate(pos.getX(), pos.getY(), pos.getZ(), getBlockId(blockUpdate.getBlockState()));
            } else if (packet instanceof ClientboundSectionBlocksUpdatePacket sectionUpdate) {
                sectionUpdate.runUpdates((pos, state) -> {
                    onServerBlockUpdate(pos.getX(), pos.getY(), pos.getZ(), getBlockId(state));
                });
            }
        } else {
            if (packet instanceof ServerboundPlayerActionPacket action) {
                ServerboundPlayerActionPacket.Action a = action.getAction();
                if (a == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK)
                    markMined(action.getPos());
            } else if (packet instanceof ServerboundUseItemOnPacket useOn) {
                var hit = useOn.getHitResult();
                if (hit != null)
                    markPlaced(hit.getBlockPos().relative(hit.getDirection()));
            }
        }
    }
    public static void onServerBlockUpdate(int x, int y, int z, String blockId) {
        if (!Modules.enabled(GhostBlocks.class)) return;
        GhostBlocks m = Modules.get(GhostBlocks.class);
        long packed = BlockUtility.packPos(x, y, z);
        m.recentlyMined.remove(packed);
        m.recentlyPlaced.remove(packed);
        m.pendingTime.remove(packed);
        if (blockId != null && !blockId.equals("minecraft:air")) {
            m.serverBlocks.put(packed, blockId);
        } else {
            m.serverBlocks.remove(packed);
        }
    }
    public static boolean isGhostBlock(int x, int y, int z, String clientBlockId) {
        if (!Modules.enabled(GhostBlocks.class)) return false;
        GhostBlocks m = Modules.get(GhostBlocks.class);
        if (m == null) return false;
        long packed = BlockUtility.packPos(x, y, z);
        if (m.breaking && m.recentlyMined.contains(packed)) return true;
        if (m.placing && m.recentlyPlaced.contains(packed)) return true;
        String serverBlock = m.serverBlocks.get(packed);
        if (serverBlock == null) return false;
        if (m.breaking && !serverBlock.equals(clientBlockId) && !"minecraft:air".equals(serverBlock)) return true;
        if (m.placing && "minecraft:air".equals(serverBlock) && !"minecraft:air".equals(clientBlockId)) return true;
        return false;
    }
    public static String getBlockId(net.minecraft.world.level.block.state.BlockState state) {
        Identifier rl = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return rl != null ? rl.toString() : "minecraft:air";
    }
}
