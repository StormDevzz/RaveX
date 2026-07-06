package ravex.modules.render;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;
import java.util.ArrayList;
import java.util.List;
public class VoidESP extends Module {
    public static final VoidESP INSTANCE = new VoidESP();
    public final NumberParameter range = new NumberParameter("Range", 32, 8, 64, 4);
    public final NumberParameter height = new NumberParameter("Height", 10, 2, 30, 2);
    public final ColorParameter voidColor = new ColorParameter("VoidColor", 0x66FF0000);
    public final BooleanParameter filled = new BooleanParameter("Filled", true);
    public final BooleanParameter wireframe = new BooleanParameter("Wireframe", true);
    public final BooleanParameter floorOnly = new BooleanParameter("FloorOnly", true);
    public final NumberParameter updateInterval = new NumberParameter("UpdateInterval", 20, 5, 100, 5);
    private List<BlockPos> voidBlocks = new ArrayList<>();
    private long lastScan = 0;

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        long now = System.currentTimeMillis();
        if (now - lastScan < updateInterval.getValue().intValue() * 50) return;
        lastScan = now;
        List<BlockPos> result = new ArrayList<>();
        BlockPos center = mc.player.blockPosition();
        int r = range.getValue().intValue();
        int h = height.getValue().intValue();
        int floorH = floorOnly.getValue() ? 1 : h;
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                for (int y = 1; y <= floorH; y++) {
                    if (center.getY() - y <= mc.level.getMinY()) continue;
                    BlockPos pos = center.offset(x, -y, z);
                    if (mc.level.getBlockState(pos).isAir()) {
                        boolean hasFloor = false;
                        for (int checkY = pos.getY() + 1; checkY <= mc.level.getMaxY(); checkY++) {
                            if (!mc.level.getBlockState(new BlockPos(pos.getX(), checkY, pos.getZ())).isAir()) {
                                hasFloor = true;
                                break;
                            }
                        }
                        if (!hasFloor) result.add(pos);
                    }
                }
            }
        }
        voidBlocks = result;
    }
    public List<BlockPos> getVoidBlocks() {
        return voidBlocks;
    }
}
