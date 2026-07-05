package ravex.modules.render;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import java.util.ArrayList;
import java.util.List;
public class ESP extends Module {
    public static final ESP INSTANCE = new ESP();
    public final ModeParameter mode = new ModeParameter("Mode", "Outline", java.util.List.of("Outline", "Box2D", "Tunnels"));
    public final BooleanParameter players = new BooleanParameter("Players", true);
    public final BooleanParameter monsters = new BooleanParameter("Monsters", true);
    public final BooleanParameter animals = new BooleanParameter("Animals", false);
    public final BooleanParameter items = new BooleanParameter("Items", false);
    public final BooleanParameter frames = new BooleanParameter("Frames", false);
    public final NumberParameter maxDistance = new NumberParameter("Distance", 100.0, 10.0, 300.0, 10.0);
    public final ColorParameter playerColor = new ColorParameter("Player Color", 0xFFFF3333);
    public final ColorParameter mobColor    = new ColorParameter("Mob Color",    0xFF33FF33);
    public final ColorParameter animalColor = new ColorParameter("Animal Color", 0xFF33FF55);
    public final ColorParameter itemColor   = new ColorParameter("Item Color",   0xFFFFFF33);
    public final ColorParameter frameColor  = new ColorParameter("Frame Color",  0xFFFF9933);
    public final NumberParameter tunnelRange = new NumberParameter("Tunnel Range", 32, 8, 64, 4);
    public final NumberParameter tunnelMaxY = new NumberParameter("Tunnel MaxY", 40, 5, 60, 5);
    public final NumberParameter tunnelMinY = new NumberParameter("Tunnel MinY", 5, 1, 30, 1);
    public final ColorParameter tunnelColor = new ColorParameter("Tunnel Color", 0x44FFFF00);
    public final BooleanParameter tunnelFilled = new BooleanParameter("Tunnel Filled", false);
    public final BooleanParameter tunnelWireframe = new BooleanParameter("Tunnel Wireframe", true);
    public final NumberParameter tunnelUpdateInterval = new NumberParameter("Tunnel Update", 20, 5, 100, 5);
    private List<BlockPos> tunnelBlocks = new ArrayList<>();
    private long lastTunnelScan = 0;
    private ESP() {
        super("ESP");
        playerColor.setVisible(players::getValue);
        mobColor.setVisible(monsters::getValue);
        animalColor.setVisible(animals::getValue);
        itemColor.setVisible(items::getValue);
        frameColor.setVisible(frames::getValue);
        tunnelRange.setVisible(() -> mode.getValue().equals("Tunnels"));
        tunnelMaxY.setVisible(() -> mode.getValue().equals("Tunnels"));
        tunnelMinY.setVisible(() -> mode.getValue().equals("Tunnels"));
        tunnelColor.setVisible(() -> mode.getValue().equals("Tunnels"));
        tunnelFilled.setVisible(() -> mode.getValue().equals("Tunnels"));
        tunnelWireframe.setVisible(() -> mode.getValue().equals("Tunnels"));
        tunnelUpdateInterval.setVisible(() -> mode.getValue().equals("Tunnels"));
    }
    @Override
    public void onTick() {
        if (!mode.getValue().equals("Tunnels")) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        long now = System.currentTimeMillis();
        if (now - lastTunnelScan < tunnelUpdateInterval.getValue().intValue() * 50) return;
        lastTunnelScan = now;
        scanTunnels(mc);
    }
    private void scanTunnels(Minecraft mc) {
        List<BlockPos> result = new ArrayList<>();
        BlockPos center = mc.player.blockPosition();
        int r = tunnelRange.getValue().intValue();
        int my = tunnelMaxY.getValue().intValue();
        int ny = tunnelMinY.getValue().intValue();
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
