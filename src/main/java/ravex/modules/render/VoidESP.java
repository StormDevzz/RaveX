package ravex.modules.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;

@Module(name = "VoidESP", category = "Render")
public class VoidESP {
    @Parameter(name = "Range", min = 8, max = 64, step = 4)
    public double voidRange = 32;

    @Parameter(name = "Color", color = true)
    public int voidColor = 0x6640A9F8;

    @Parameter(name = "Filled")
    public boolean voidFilled = true;

    @Parameter(name = "Wireframe")
    public boolean voidWireframe = true;

    @Parameter(name = "Update", min = 5, max = 100, step = 5)
    public double voidUpdateInterval = 20;

    private List<BlockPos> voidBlocks = new ArrayList<>();
    private long lastVoidScan = 0;

    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) return;
        long now = System.currentTimeMillis();
        if (now - lastVoidScan < (int) voidUpdateInterval * 50L) return;
        lastVoidScan = now;
        var level = mc.getLevel();
        List<BlockPos> result = new ArrayList<>();
        BlockPos center = mc.getPlayer().blockPosition();
        int r = (int) voidRange;
        int minY = level.getMinY();
        int topY = center.getY() - 1;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                int bx = center.getX() + x;
                int bz = center.getZ() + z;
                // first solid block closes the column above it
                int topOpen = -1;
                for (int y = minY; y <= topY; y++) {
                    cursor.set(bx, y, bz);
                    if (!level.getBlockState(cursor).isAir()) break;
                    topOpen = y;
                }
                if (topOpen != -1) result.add(new BlockPos(bx, topOpen, bz));
            }
        }
        voidBlocks = result;
    }

    public List<BlockPos> getVoidBlocks() {
        return voidBlocks;
    }
}
