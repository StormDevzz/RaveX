package ravex.modules.world;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.misc.PhysicUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.SwingUtility;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import java.util.HashSet;
import java.util.Set;
import ravex.mcwrapper.MinecraftWrapper;




@Module(name = "AutoReplant", category = "World")
public class AutoReplant {
    @Parameter(name = "Range", min = 1.0, max = 6.0, step = 0.5)
    public double range = 4.0;
    @Parameter(name = "Delay", min = 100, max = 1000, step = 50)
    public double delay = 300;
    @Parameter(name = "SilentSwap")
    public boolean silent = true;
    private long lastReplantTime = 0;
    private static final Set<Block> farmBlocks = new HashSet<>();
    static {
        farmBlocks.add(net.minecraft.world.level.block.Blocks.FARMLAND);
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) return;
        long now = System.currentTimeMillis();
        if (now - lastReplantTime < delay) return;
        int seedSlot = findSeedSlot(mc);
        if (seedSlot == -1) return;
        double r = range;
        net.minecraft.core.BlockPos playerPos = mc.getPlayer().blockPosition();
        int minX = (int) Math.floor(playerPos.getX() - r);
        int maxX = (int) Math.ceil(playerPos.getX() + r);
        int minY = (int) Math.floor(playerPos.getY() - r);
        int maxY = (int) Math.ceil(playerPos.getY() + r);
        int minZ = (int) Math.floor(playerPos.getZ() - r);
        int maxZ = (int) Math.ceil(playerPos.getZ() + r);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x, y, z);
                    net.minecraft.world.level.block.state.BlockState state = mc.getLevel().getBlockState(pos);
                    if (!state.is(net.minecraft.world.level.block.Blocks.FARMLAND)) continue;
                    if (!mc.getLevel().getBlockState(pos.above()).isAir()) continue;
                    if (PhysicUtility.centerOf(pos).distanceToSqr(mc.getPlayer().getEyePosition()) > r * r) continue;
                    int prevSlot = InventoryUtility.getSelectedSlot(mc.getPlayer());
                    InventoryUtility.selectSlot(mc.getPlayer(), seedSlot);
                    net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(
                        PhysicUtility.centerOf(pos), net.minecraft.core.Direction.UP, pos, false
                    );
                    mc.getGameMode().useItemOn(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND, hit);
                    if (silent) {
                        InventoryUtility.selectSlot(mc.getPlayer(), prevSlot);
                    }
                    lastReplantTime = now;
                    return;
                }
            }
        }
    }
    private int findSeedSlot(MinecraftWrapper mc) {
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.getPlayer(), i);
            if (!stack.isEmpty()) {
                Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (id != null && id.getNamespace().equals("minecraft") &&
                    (id.getPath().endsWith("_seeds") || id.getPath().contains("seed"))) {
                    return i;
                }
            }
        }
        return -1;
    }



}