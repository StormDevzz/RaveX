package ravex.modules.render;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.block.BlockUtility;
import net.minecraft.client.Minecraft;
import java.util.ArrayList;
import java.util.List;




@ModuleInfo(name = "ESP", category = "Render")
public class ESP implements ModuleAccess {
    @Parameter(name = "Mode", modes = {"Outline", "Box2D", "Tunnels", "Holes", "Void"})
    public String mode = "Outline";
    @Parameter(name = "Players")
    public boolean players = true;
    @Parameter(name = "Monsters")
    public boolean monsters = true;
    @Parameter(name = "Animals")
    public boolean animals = false;
    @Parameter(name = "Items")
    public boolean items = false;
    @Parameter(name = "Frames")
    public boolean frames = false;
    @Parameter(name = "Distance", min = 10.0, max = 300.0, step = 10.0)
    public double maxDistance = 100.0;
    @Parameter(name = "PlayerColor", color = true)
    public int playerColor = 0xFFFF3333;
    @Parameter(name = "MobColor", color = true)
    public int mobColor = 0xFF33FF33;
    @Parameter(name = "AnimalColor", color = true)
    public int animalColor = 0xFF33FF55;
    @Parameter(name = "ItemColor", color = true)
    public int itemColor = 0xFFFFFF33;
    @Parameter(name = "FrameColor", color = true)
    public int frameColor = 0xFFFF9933;
    @Parameter(name = "TunnelRange", min = 8, max = 64, step = 4)
    public double tunnelRange = 32;
    @Parameter(name = "TunnelMaxY", min = 5, max = 60, step = 5)
    public double tunnelMaxY = 40;
    @Parameter(name = "TunnelMinY", min = 1, max = 30, step = 1)
    public double tunnelMinY = 5;
    @Parameter(name = "TunnelColor", color = true)
    public int tunnelColor = 0x44FFFF00;
    @Parameter(name = "TunnelFilled")
    public boolean tunnelFilled = false;
    @Parameter(name = "TunnelWireframe")
    public boolean tunnelWireframe = true;
    @Parameter(name = "TunnelUpdate", min = 5, max = 100, step = 5)
    public double tunnelUpdateInterval = 20;
    @Parameter(name = "HoleRange", min = 4, max = 24, step = 2)
    public double holeRange = 8;
    @Parameter(name = "SafeColor", color = true)
    public int safeColor = 0xAA00FF00;
    @Parameter(name = "HoleFilled")
    public boolean holeFilled = true;
    @Parameter(name = "HoleWireframe")
    public boolean holeWireframe = true;
    @Parameter(name = "VoidRange", min = 8, max = 64, step = 4)
    public double voidRange = 32;
    @Parameter(name = "VoidHeight", min = 2, max = 30, step = 2)
    public double voidHeight = 10;
    @Parameter(name = "VoidColor", color = true)
    public int voidColor = 0x66FF0000;
    @Parameter(name = "VoidFilled")
    public boolean voidFilled = true;
    @Parameter(name = "VoidWireframe")
    public boolean voidWireframe = true;
    @Parameter(name = "VoidFloorOnly")
    public boolean voidFloorOnly = true;
    @Parameter(name = "VoidUpdate", min = 5, max = 100, step = 5)
    public double voidUpdateInterval = 20;
    private List<net.minecraft.core.BlockPos> tunnelBlocks = new ArrayList<>();
    private long lastTunnelScan = 0;
    private final List<net.minecraft.core.BlockPos> holes = new ArrayList<>();
    private int holeTick = 0;
    private List<net.minecraft.core.BlockPos> voidBlocks = new ArrayList<>();
    private long lastVoidScan = 0;
    private ESP() {
        
    }
    public void onTick() {
        String m = mode;
        if (m.equals("Tunnels")) scanTunnels();
        else if (m.equals("Holes")) scanHoles();
        else if (m.equals("Void")) scanVoid();
    }
    private void scanTunnels() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        long now = System.currentTimeMillis();
        if (now - lastTunnelScan < (int) tunnelUpdateInterval * 50) return;
        lastTunnelScan = now;
        List<net.minecraft.core.BlockPos> result = new ArrayList<>();
        net.minecraft.core.BlockPos center = mc.player.blockPosition();
        int r = (int) tunnelRange;
        int my = (int) tunnelMaxY;
        int ny = (int) tunnelMinY;
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                for (int y = ny; y <= my; y++) {
                    net.minecraft.core.BlockPos pos = center.offset(x, y, z);
                    if (!mc.level.getBlockState(pos).isAir()) continue;
                    if (!mc.level.getBlockState(pos.above()).isAir()) continue;
                    if (mc.level.getBlockState(pos.below()).isAir()) continue;
                    if (mc.level.getBlockState(pos.above(2)).isAir()) continue;
                    net.minecraft.world.level.block.state.BlockState west = mc.level.getBlockState(pos.west());
                    net.minecraft.world.level.block.state.BlockState east = mc.level.getBlockState(pos.east());
                    net.minecraft.world.level.block.state.BlockState north = mc.level.getBlockState(pos.north());
                    net.minecraft.world.level.block.state.BlockState south = mc.level.getBlockState(pos.south());
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
        net.minecraft.core.BlockPos center = mc.player.blockPosition();
        int r = (int) holeRange;
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                for (int y = -4; y <= 2; y++) {
                    net.minecraft.core.BlockPos pos = center.offset(x, y, z);
                    if (!mc.level.getBlockState(pos).isAir()) continue;
                    if (isSafeHole(mc, pos)) holes.add(pos);
                }
            }
        }
    }
    private boolean isSafeHole(Minecraft mc, net.minecraft.core.BlockPos pos) {
        if (!mc.level.getBlockState(pos.below()).isSolid()) return false;
        net.minecraft.core.BlockPos[] sides = {
            pos.east(), pos.west(), pos.south(), pos.north()
        };
        for (net.minecraft.core.BlockPos side : sides) {
            net.minecraft.world.level.block.state.BlockState state = mc.level.getBlockState(side);
            if (state.isAir() || !state.isSolid()) return false;
        }
        return true;
    }
    private void scanVoid() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        long now = System.currentTimeMillis();
        if (now - lastVoidScan < (int) voidUpdateInterval * 50) return;
        lastVoidScan = now;
        List<net.minecraft.core.BlockPos> result = new ArrayList<>();
        net.minecraft.core.BlockPos center = mc.player.blockPosition();
        int r = (int) voidRange;
        int h = (int) voidHeight;
        int floorH = voidFloorOnly ? 1 : h;
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                for (int y = 1; y <= floorH; y++) {
                    if (center.getY() - y <= mc.level.getMinY()) continue;
                    net.minecraft.core.BlockPos pos = center.offset(x, -y, z);
                    if (mc.level.getBlockState(pos).isAir()) {
                        boolean hasFloor = false;
                        for (int checkY = pos.getY() + 1; checkY <= mc.level.getMaxY(); checkY++) {
                            if (!mc.level.getBlockState(new net.minecraft.core.BlockPos(pos.getX(), checkY, pos.getZ())).isAir()) {
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
    public static boolean shouldGlow(net.minecraft.world.entity.Entity entity) {
        ESP $ = ravex.manager.ModuleManager.delegate(ESP.class);
        if ($ == null || !$.getEnabled() || !$.mode.equals("Outline")) return false;
        var mc = Minecraft.getInstance();
        if (entity == mc.player) return false;
        if (mc.player != null && mc.player.distanceTo(entity) > $.maxDistance) return false;
        if (entity instanceof net.minecraft.world.entity.player.Player && $.players) return true;
        if (entity instanceof net.minecraft.world.entity.monster.Monster && $.monsters) return true;
        if ((entity instanceof net.minecraft.world.entity.animal.Animal || entity instanceof net.minecraft.world.entity.ambient.AmbientCreature) && $.animals) return true;
        if (entity instanceof net.minecraft.world.entity.item.ItemEntity && $.items) return true;
        if (entity instanceof net.minecraft.world.entity.decoration.ItemFrame && $.frames) return true;
        return false;
    }

    public List<net.minecraft.core.BlockPos> getTunnelBlocks() { return tunnelBlocks; }
    public List<net.minecraft.core.BlockPos> getHoles() { return holes; }
    public List<net.minecraft.core.BlockPos> getVoidBlocks() { return voidBlocks; }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("ESP").getEnabled();
    }

    public static ESP itz() {
        return ravex.manager.ModuleManager.delegate(ESP.class);
    }


}