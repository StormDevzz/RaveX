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
    public final ModeParameter mode = new ModeParameter("Mode", "Outline", java.util.List.of("Outline", "Box2D", "Tunnels", "Holes"));
    public final BooleanParameter players = new BooleanParameter("Players", true);
    public final BooleanParameter monsters = new BooleanParameter("Monsters", true);
    public final BooleanParameter animals = new BooleanParameter("Animals", false);
    public final BooleanParameter items = new BooleanParameter("Items", false);
    public final BooleanParameter frames = new BooleanParameter("Frames", false);
    public final NumberParameter maxDistance = new NumberParameter("Distance", 100.0, 10.0, 300.0, 10.0);
    public final ColorParameter playerColor = new ColorParameter("PlayerColor", 0xFFFF3333);
    public final ColorParameter mobColor    = new ColorParameter("MobColor",    0xFF33FF33);
    public final ColorParameter animalColor = new ColorParameter("AnimalColor", 0xFF33FF55);
    public final ColorParameter itemColor   = new ColorParameter("ItemColor",   0xFFFFFF33);
    public final ColorParameter frameColor  = new ColorParameter("FrameColor",  0xFFFF9933);
    public final NumberParameter tunnelRange = new NumberParameter("TunnelRange", 32, 8, 64, 4);
    public final NumberParameter tunnelMaxY = new NumberParameter("TunnelMaxY", 40, 5, 60, 5);
    public final NumberParameter tunnelMinY = new NumberParameter("TunnelMinY", 5, 1, 30, 1);
    public final ColorParameter tunnelColor = new ColorParameter("TunnelColor", 0x44FFFF00);
    public final BooleanParameter tunnelFilled = new BooleanParameter("TunnelFilled", false);
    public final BooleanParameter tunnelWireframe = new BooleanParameter("TunnelWireframe", true);
    public final NumberParameter tunnelUpdateInterval = new NumberParameter("TunnelUpdate", 20, 5, 100, 5);
    public final NumberParameter holeRange = new NumberParameter("HoleRange", 8, 4, 24, 2);
    public final ColorParameter safeColor = new ColorParameter("SafeColor", 0xAA00FF00);
    public final BooleanParameter holeFilled = new BooleanParameter("HoleFilled", true);
    public final BooleanParameter holeWireframe = new BooleanParameter("HoleWireframe", true);
    private List<BlockPos> tunnelBlocks = new ArrayList<>();
    private long lastTunnelScan = 0;
    private final List<BlockPos> holes = new ArrayList<>();
    private int holeTick = 0;
    private ESP() {
        super("ESP");
        playerColor.setVisible(players::getValue);
        mobColor.setVisible(monsters::getValue);
        animalColor.setVisible(animals::getValue);
        itemColor.setVisible(items::getValue);
        frameColor.setVisible(frames::getValue);
        boolean t = mode.getValue().equals("Tunnels");
        tunnelRange.setVisible(() -> mode.getValue().equals("Tunnels"));
        tunnelMaxY.setVisible(() -> mode.getValue().equals("Tunnels"));
        tunnelMinY.setVisible(() -> mode.getValue().equals("Tunnels"));
        tunnelColor.setVisible(() -> mode.getValue().equals("Tunnels"));
        tunnelFilled.setVisible(() -> mode.getValue().equals("Tunnels"));
        tunnelWireframe.setVisible(() -> mode.getValue().equals("Tunnels"));
        tunnelUpdateInterval.setVisible(() -> mode.getValue().equals("Tunnels"));
        holeRange.setVisible(() -> mode.getValue().equals("Holes"));
        safeColor.setVisible(() -> mode.getValue().equals("Holes"));
        holeFilled.setVisible(() -> mode.getValue().equals("Holes"));
        holeWireframe.setVisible(() -> mode.getValue().equals("Holes"));
    }
    @Override
    public void onTick() {
        String m = mode.getValue();
        if (m.equals("Tunnels")) scanTunnels();
        else if (m.equals("Holes")) scanHoles();
    }
    private void scanTunnels() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        long now = System.currentTimeMillis();
        if (now - lastTunnelScan < tunnelUpdateInterval.getValue().intValue() * 50) return;
        lastTunnelScan = now;
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
    private void scanHoles() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (++holeTick % 5 != 0) return;
        holeTick = 0;
        holes.clear();
        BlockPos center = mc.player.blockPosition();
        int r = holeRange.getValue().intValue();
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                for (int y = -4; y <= 2; y++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (!mc.level.getBlockState(pos).isAir()) continue;
                    if (isSafeHole(mc, pos)) holes.add(pos);
                }
            }
        }
    }
    private boolean isSafeHole(Minecraft mc, BlockPos pos) {
        if (!mc.level.getBlockState(pos.below()).isSolid()) return false;
        BlockPos[] sides = {
            pos.east(), pos.west(), pos.south(), pos.north()
        };
        for (BlockPos side : sides) {
            BlockState state = mc.level.getBlockState(side);
            if (state.isAir() || !state.isSolid()) return false;
        }
        return true;
    }
    public List<BlockPos> getTunnelBlocks() { return tunnelBlocks; }
    public List<BlockPos> getHoles() { return holes; }
}
