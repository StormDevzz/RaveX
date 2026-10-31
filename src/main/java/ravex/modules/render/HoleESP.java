package ravex.modules.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;

@Module(name = "HoleESP", category = "Render")
public class HoleESP {
    @Parameter(name = "Range", min = 4, max = 24, step = 2)
    public double holeRange = 8;

    @Parameter(name = "SafeColor", color = true)
    public int safeColor = 0xAA00FF00;

    @Parameter(name = "Filled")
    public boolean holeFilled = true;

    @Parameter(name = "Wireframe")
    public boolean holeWireframe = true;

    private final List<BlockPos> holes = new ArrayList<>();
    private int holeTick = 0;

    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) return;
        if (++holeTick % 5 != 0) return;
        holeTick = 0;
        holes.clear();
        BlockPos center = mc.getPlayer().blockPosition();
        int r = (int) holeRange;
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                for (int y = -4; y <= 2; y++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (!mc.getLevel().getBlockState(pos).isAir()) continue;
                    if (isSafeHole(mc, pos)) holes.add(pos);
                }
            }
        }
    }

    private boolean isSafeHole(MinecraftWrapper mc, BlockPos pos) {
        if (!mc.getLevel().getBlockState(pos.below()).isSolid()) return false;
        BlockPos[] sides = { pos.east(), pos.west(), pos.south(), pos.north() };
        for (BlockPos side : sides) {
            BlockState state = mc.getLevel().getBlockState(side);
            if (state.isAir() || !state.isSolid()) return false;
        }
        return true;
    }

    public List<BlockPos> getHoles() {
        return holes;
    }
}
