package ravex.modules.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;

@Module(name = "TunnelESP", category = "Render")
public class TunnelESP {
    @Parameter(name = "Range", min = 8, max = 64, step = 4)
    public double tunnelRange = 32;

    @Parameter(name = "MaxY", min = 5, max = 60, step = 5)
    public double tunnelMaxY = 40;

    @Parameter(name = "MinY", min = 1, max = 30, step = 1)
    public double tunnelMinY = 5;

    @Parameter(name = "Color", color = true)
    public int tunnelColor = 0x4440A9F8;

    @Parameter(name = "Filled")
    public boolean tunnelFilled = false;

    @Parameter(name = "Wireframe")
    public boolean tunnelWireframe = true;

    @Parameter(name = "Update", min = 5, max = 100, step = 5)
    public double tunnelUpdateInterval = 20;

    private List<BlockPos> tunnelBlocks = new ArrayList<>();
    private long lastTunnelScan = 0;

    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) return;
        long now = System.currentTimeMillis();
        if (now - lastTunnelScan < (int) tunnelUpdateInterval * 50L) return;
        lastTunnelScan = now;
        List<BlockPos> result = new ArrayList<>();
        BlockPos center = mc.getPlayer().blockPosition();
        int r = (int) tunnelRange;
        int my = (int) tunnelMaxY;
        int ny = (int) tunnelMinY;
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                for (int y = ny; y <= my; y++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (!mc.getLevel().getBlockState(pos).isAir()) continue;
                    if (!mc.getLevel().getBlockState(pos.above()).isAir()) continue;
                    if (mc.getLevel().getBlockState(pos.below()).isAir()) continue;
                    if (mc.getLevel().getBlockState(pos.above(2)).isAir()) continue;
                    boolean wallsEW = !mc.getLevel().getBlockState(pos.west()).isAir()
                            && !mc.getLevel().getBlockState(pos.east()).isAir();
                    boolean wallsNS = !mc.getLevel().getBlockState(pos.north()).isAir()
                            && !mc.getLevel().getBlockState(pos.south()).isAir();
                    if (wallsEW || wallsNS) {
                        result.add(pos);
                        result.add(pos.above());
                    }
                }
            }
        }
        tunnelBlocks = result;
    }

    public List<BlockPos> getTunnelBlocks() {
        return tunnelBlocks;
    }
}
