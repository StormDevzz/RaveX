package ravex.modules.world;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.nativelib.NativeLibrary;
import java.util.HashSet;
import java.util.Set;
public class NoGhostBlocks extends Module {
    public static final NoGhostBlocks INSTANCE = new NoGhostBlocks();
    public final ModeParameter mode = new ModeParameter("Mode", "Strict", java.util.List.of("Strict", "Smooth"));
    public final NumberParameter range = new NumberParameter("Range", 6.0, 2.0, 12.0, 0.5);
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_noghostblocks");
    static {
        NATIVE.load();
    }
    private final Set<BlockPos> recentlyMined = new HashSet<>();
    private long lastCheckTime = 0;

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getConnection() == null) return;
        long now = System.currentTimeMillis();
        if (now - lastCheckTime < 500) return;
        lastCheckTime = now;
        double r = range.getValue();
        BlockPos pPos = mc.player.blockPosition();
        int minX = (int) Math.floor(pPos.getX() - r);
        int maxX = (int) Math.ceil(pPos.getX() + r);
        int minY = (int) Math.max(mc.level.getMinY(), Math.floor(pPos.getY() - r));
        int maxY = (int) Math.min(mc.level.getMaxY(), Math.ceil(pPos.getY() + r));
        int minZ = (int) Math.floor(pPos.getZ() - r);
        int maxZ = (int) Math.ceil(pPos.getZ() + r);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = mc.level.getBlockState(pos);
                    if (state.isAir()) continue;
                    if (state.getDestroySpeed(mc.level, pos) < 0) continue;
                    boolean isGhost = false;
                    if (NATIVE.isLoaded()) {
                        isGhost = nativeIsGhostBlock(x, y, z, getBlockId(state));
                    } else if (recentlyMined.contains(pos)) {
                        isGhost = true;
                    }
                    if (isGhost && "Strict".equals(mode.getValue())) {
                        mc.getConnection().send(
                            new ServerboundPlayerActionPacket(
                                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                                pos, Direction.UP, 0
                            )
                        );
                        mc.getConnection().send(
                            new ServerboundPlayerActionPacket(
                                ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                                pos, Direction.UP, 0
                            )
                        );
                        recentlyMined.remove(pos);
                        mc.player.swing(InteractionHand.MAIN_HAND);
                    }
                }
            }
        }
    }
    public static void markMined(BlockPos pos) {
        if (INSTANCE.getEnabled()) {
            INSTANCE.recentlyMined.add(pos);
        }
    }
    public static void onServerBlockUpdate(int x, int y, int z, String blockId) {
        if (!INSTANCE.getEnabled()) return;
        BlockPos pos = new BlockPos(x, y, z);
        INSTANCE.recentlyMined.remove(pos);
    }
    public static boolean isGhostBlock(int x, int y, int z, String clientBlockId) {
        if (!INSTANCE.getEnabled()) return false;
        if (NATIVE.isLoaded()) {
            return nativeIsGhostBlock(x, y, z, clientBlockId);
        }
        return INSTANCE.recentlyMined.contains(new BlockPos(x, y, z));
    }
    public static String getBlockId(BlockState state) {
        Identifier rl = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return rl != null ? rl.toString() : "minecraft:air";
    }
    private static native boolean nativeIsGhostBlock(int x, int y, int z, String clientBlockId);
    private static native void nativeReset();
}
