package ravex.modules.esp;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;

import java.util.ArrayList;
import java.util.List;

public class HoleESP extends Module {
    public static final HoleESP INSTANCE = new HoleESP();

    public final NumberParameter range = new NumberParameter("Range", 8, 4, 24, 2);
    public final ColorParameter safeColor = new ColorParameter("SafeColor", 0xAA00FF00);
    public final BooleanParameter filled = new BooleanParameter("Filled", true);
    public final BooleanParameter wireframe = new BooleanParameter("Wireframe", true);

    private final List<BlockPos> holes = new ArrayList<>();
    private int tickCounter = 0;

    private HoleESP() {
        super("HoleESP", Category.RENDER);
        addParameter(range);
        addParameter(safeColor);
        addParameter(filled);
        addParameter(wireframe);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (++tickCounter % 5 != 0) return;
        tickCounter = 0;

        holes.clear();
        BlockPos center = mc.player.blockPosition();
        int r = range.getValue().intValue();

        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                for (int y = -4; y <= 2; y++) {
                    BlockPos pos = center.offset(x, y, 0).offset(0, 0, z);
                    if (!mc.level.getBlockState(pos).isAir()) continue;
                    if (isSafeHole(mc, pos)) {
                        holes.add(pos);
                    }
                }
            }
        }
    }

    private boolean isSafeHole(Minecraft mc, BlockPos pos) {
        if (!mc.level.getBlockState(pos.below()).isSolid()) return false;
        BlockPos[] sides = {
            pos.offset(1, 0, 0), pos.offset(-1, 0, 0),
            pos.offset(0, 0, 1), pos.offset(0, 0, -1)
        };
        for (BlockPos side : sides) {
            BlockState state = mc.level.getBlockState(side);
            if (state.isAir() || !state.isSolid()) return false;
        }
        return true;
    }

    public List<BlockPos> getHoles() {
        return holes;
    }
}
