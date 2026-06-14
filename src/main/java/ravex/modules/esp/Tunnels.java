package ravex.modules.esp;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;

import java.util.ArrayList;
import java.util.List;

public class Tunnels extends Module {
    public static final Tunnels INSTANCE = new Tunnels();

    public final NumberParameter range = new NumberParameter("Range", 32, 8, 64, 4);
    public final NumberParameter maxY = new NumberParameter("MaxY", 40, 5, 60, 5);
    public final NumberParameter minY = new NumberParameter("MinY", 5, 1, 30, 1);
    public final ColorParameter tunnelColor = new ColorParameter("TunnelColor", 0x44FFFF00);
    public final BooleanParameter filled = new BooleanParameter("Filled", false);
    public final BooleanParameter wireframe = new BooleanParameter("Wireframe", true);
    public final NumberParameter updateInterval = new NumberParameter("UpdateInterval", 20, 5, 100, 5);

    private List<BlockPos> tunnelBlocks = new ArrayList<>();
    private long lastScan = 0;

    private Tunnels() {
        super("Tunnels", Category.RENDER);
        addParameter(range);
        addParameter(maxY);
        addParameter(minY);
        addParameter(tunnelColor);
        addParameter(filled);
        addParameter(wireframe);
        addParameter(updateInterval);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        long now = System.currentTimeMillis();
        if (now - lastScan < updateInterval.getValue().intValue() * 50) return;
        lastScan = now;

        scanTunnelsJava(mc);
    }

    private void scanTunnelsJava(Minecraft mc) {
        List<BlockPos> result = new ArrayList<>();
        BlockPos center = mc.player.blockPosition();
        int r = range.getValue().intValue();
        int my = maxY.getValue().intValue();
        int ny = minY.getValue().intValue();

        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                for (int y = ny; y <= my; y++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (!mc.level.getBlockState(pos).isAir()) continue;
                    if (!mc.level.getBlockState(pos.above()).isAir()) continue;
                    if (mc.level.getBlockState(pos.below()).isAir()) continue;
                    if (mc.level.getBlockState(pos.above(2)).isAir()) continue;
                    BlockState west = mc.level.getBlockState(pos.west());
                    BlockState east = mc.level.getBlockState(pos.east());
                    BlockState north = mc.level.getBlockState(pos.north());
                    BlockState south = mc.level.getBlockState(pos.south());
                    boolean wallsEW = !west.isAir() && !east.isAir();
                    boolean wallsNS = !north.isAir() && !south.isAir();
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
