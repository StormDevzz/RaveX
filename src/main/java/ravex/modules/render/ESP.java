package ravex.modules.render;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
import ravex.utility.misc.block.BlockUtility;
import net.minecraft.world.level.block.state.BlockState;

import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import java.util.ArrayList;
import java.util.List;
@ModuleInfo(name = "ESP", category = "Render")
public class ESP extends ravex.modules.Module {
public final ModeParameter mode = new ModeParameter("Mode", "Outline", java.util.List.of("Outline", "Box2D", "Tunnels", "Holes", "Void"));
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
    public final NumberParameter voidRange = new NumberParameter("VoidRange", 32, 8, 64, 4);
    public final NumberParameter voidHeight = new NumberParameter("VoidHeight", 10, 2, 30, 2);
    public final ColorParameter voidColor = new ColorParameter("VoidColor", 0x66FF0000);
    public final BooleanParameter voidFilled = new BooleanParameter("VoidFilled", true);
    public final BooleanParameter voidWireframe = new BooleanParameter("VoidWireframe", true);
    public final BooleanParameter voidFloorOnly = new BooleanParameter("VoidFloorOnly", true);
    public final NumberParameter voidUpdateInterval = new NumberParameter("VoidUpdate", 20, 5, 100, 5);
    private List<net.minecraft.core.BlockPos> tunnelBlocks = new ArrayList<>();
    private long lastTunnelScan = 0;
    private final List<net.minecraft.core.BlockPos> holes = new ArrayList<>();
    private int holeTick = 0;
    private List<net.minecraft.core.BlockPos> voidBlocks = new ArrayList<>();
    private long lastVoidScan = 0;
    private ESP() {
        
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
        holeRange.setVisible(() -> mode.getValue().equals("Holes"));
        safeColor.setVisible(() -> mode.getValue().equals("Holes"));
        holeFilled.setVisible(() -> mode.getValue().equals("Holes"));
        holeWireframe.setVisible(() -> mode.getValue().equals("Holes"));
        voidRange.setVisible(() -> mode.getValue().equals("Void"));
        voidHeight.setVisible(() -> mode.getValue().equals("Void"));
        voidColor.setVisible(() -> mode.getValue().equals("Void"));
        voidFilled.setVisible(() -> mode.getValue().equals("Void"));
        voidWireframe.setVisible(() -> mode.getValue().equals("Void"));
        voidFloorOnly.setVisible(() -> mode.getValue().equals("Void"));
        voidUpdateInterval.setVisible(() -> mode.getValue().equals("Void"));
    }
    public void onTick() {
        String m = mode.getValue();
        if (m.equals("Tunnels")) scanTunnels();
        else if (m.equals("Holes")) scanHoles();
        else if (m.equals("Void")) scanVoid();
    }
    private void scanTunnels() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        long now = System.currentTimeMillis();
        if (now - lastTunnelScan < tunnelUpdateInterval.getValue().intValue() * 50) return;
        lastTunnelScan = now;
        List<net.minecraft.core.BlockPos> result = new ArrayList<>();
        net.minecraft.core.BlockPos center = mc.player.blockPosition();
        int r = tunnelRange.getValue().intValue();
        int my = tunnelMaxY.getValue().intValue();
        int ny = tunnelMinY.getValue().intValue();
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                for (int y = ny; y <= my; y++) {
                    net.minecraft.core.BlockPos pos = center.offset(x, y, z);
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
        net.minecraft.core.BlockPos center = mc.player.blockPosition();
        int r = holeRange.getValue().intValue();
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
            BlockState state = mc.level.getBlockState(side);
            if (state.isAir() || !state.isSolid()) return false;
        }
        return true;
    }
    private void scanVoid() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        long now = System.currentTimeMillis();
        if (now - lastVoidScan < voidUpdateInterval.getValue().intValue() * 50) return;
        lastVoidScan = now;
        List<net.minecraft.core.BlockPos> result = new ArrayList<>();
        net.minecraft.core.BlockPos center = mc.player.blockPosition();
        int r = voidRange.getValue().intValue();
        int h = voidHeight.getValue().intValue();
        int floorH = voidFloorOnly.getValue() ? 1 : h;
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
        if ($ == null || !$.getEnabled() || !$.mode.getValue().equals("Outline")) return false;
        var mc = Minecraft.getInstance();
        if (entity == mc.player) return false;
        if (mc.player != null && mc.player.distanceTo(entity) > $.maxDistance.getValue()) return false;
        if (entity instanceof net.minecraft.world.entity.player.Player && $.players.getValue()) return true;
        if (entity instanceof net.minecraft.world.entity.monster.Monster && $.monsters.getValue()) return true;
        if ((entity instanceof net.minecraft.world.entity.animal.Animal || entity instanceof net.minecraft.world.entity.ambient.AmbientCreature) && $.animals.getValue()) return true;
        if (entity instanceof net.minecraft.world.entity.item.ItemEntity && $.items.getValue()) return true;
        if (entity instanceof net.minecraft.world.entity.decoration.ItemFrame && $.frames.getValue()) return true;
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

    public java.util.List<ravex.parameter.Parameter<?>> getParameters() {
        java.util.List<ravex.parameter.Parameter<?>> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (ravex.parameter.Parameter.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    list.add((ravex.parameter.Parameter<?>) field.get(this));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }
}