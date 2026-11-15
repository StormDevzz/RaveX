package ravex.modules.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;

@Module(name = "TunnelESP", category = "Render")
public class TunnelESP {
    private static final BlockPos.MutableBlockPos BP = new BlockPos.MutableBlockPos();

    @Parameter(name = "Range", min = 8, max = 128, step = 4)
    public double tunnelRange = 48;

    @Parameter(name = "MaxY", min = -64, max = 320, step = 8)
    public double tunnelMaxY = 320;

    @Parameter(name = "MinY", min = -64, max = 320, step = 8)
    public double tunnelMinY = -64;

    @Parameter(name = "Color", color = true)
    public int tunnelColor = 0x9940A9F8;

    @Parameter(name = "Filled")
    public boolean tunnelFilled = true;

    @Parameter(name = "Wireframe")
    public boolean tunnelWireframe = true;

    @Parameter(name = "Update", min = 5, max = 60, step = 5)
    public double tunnelUpdateInterval = 10;

    @Parameter(name = "ThroughWalls")
    public boolean tunnelThroughWalls = true;

    @Parameter(name = "MaxBlocks", min = 64, max = 4096, step = 64)
    public double tunnelMaxBlocks = 1536;

    private volatile List<BlockPos> tunnelBlocks = Collections.emptyList();
    private int tunnelTick = 0;

    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) {
            tunnelBlocks = Collections.emptyList();
            return;
        }
        int interval = Math.max(1, (int) tunnelUpdateInterval);
        if (++tunnelTick % interval != 0) return;
        tunnelTick = 0;

        List<BlockPos> result = new ArrayList<>();
        BlockPos center = mc.getPlayer().blockPosition();
        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();
        int r = (int) tunnelRange;
        int minY = Math.min((int) tunnelMinY, (int) tunnelMaxY);
        int maxY = Math.max((int) tunnelMinY, (int) tunnelMaxY);
        int bottom = Math.max(minY, mc.getLevel().getMinY());
        int top = Math.min(maxY, mc.getLevel().getMaxY() - 2);
        int limit = Math.max(1, (int) tunnelMaxBlocks);
        int ySpan = Math.max(0, top - bottom);

        for (int dist = 0; dist <= r && result.size() < limit; dist++) {
            for (int dx = -dist; dx <= dist && result.size() < limit; dx++) {
                for (int dz = -dist; dz <= dist && result.size() < limit; dz++) {
                    if (Math.abs(dx) != dist && Math.abs(dz) != dist) continue;
                    int x = cx + dx;
                    int z = cz + dz;
                    for (int dy = 0; dy <= ySpan && result.size() < limit; dy++) {
                        int y1 = cy + dy;
                        int y2 = cy - dy - 1;
                        if (y1 >= bottom && y1 <= top && isTunnel(mc, x, y1, z)) {
                            result.add(new BlockPos(x, y1, z));
                        }
                        if (result.size() >= limit) break;
                        if (dy != 0 && y2 >= bottom && y2 <= top && isTunnel(mc, x, y2, z)) {
                            result.add(new BlockPos(x, y2, z));
                        }
                    }
                }
            }
        }
        tunnelBlocks = result;
    }

    private boolean isTunnel(MinecraftWrapper mc, int x, int y, int z) {
        if (!canStandIn(mc, x, y, z)) return false;

        boolean openN = canStandIn(mc, x, y, z - 1);
        boolean openS = canStandIn(mc, x, y, z + 1);
        boolean openW = canStandIn(mc, x - 1, y, z);
        boolean openE = canStandIn(mc, x + 1, y, z);

        boolean wallN = isSolidColumn(mc, x, y, z - 1);
        boolean wallS = isSolidColumn(mc, x, y, z + 1);
        boolean wallW = isSolidColumn(mc, x - 1, y, z);
        boolean wallE = isSolidColumn(mc, x + 1, y, z);

        // 1-wide corridor: walled on sides, open passage along axis (length >= 2)
        if (wallW && wallE && (openN || openS) && corridorLen(mc, x, y, z, 0, -1, 0, 1) >= 2) return true;
        if (wallN && wallS && (openW || openE) && corridorLen(mc, x, y, z, -1, 0, 1, 0) >= 2) return true;

        // 2-wide corridors
        if (wallW && openE && canStandIn(mc, x + 1, y, z) && isSolidColumn(mc, x + 2, y, z)) {
            if (corridorLen2(mc, x, y, z, 0, -1, 0, 1, true) >= 2) return true;
        }
        if (openW && wallE && canStandIn(mc, x - 1, y, z) && isSolidColumn(mc, x - 2, y, z)) {
            if (corridorLen2(mc, x, y, z, 0, -1, 0, 1, false) >= 2) return true;
        }
        if (wallN && openS && canStandIn(mc, x, y, z + 1) && isSolidColumn(mc, x, y, z + 2)) {
            if (corridorLen2(mc, x, y, z, -1, 0, 1, 0, true) >= 2) return true;
        }
        if (openN && wallS && canStandIn(mc, x, y, z - 1) && isSolidColumn(mc, x, y, z - 2)) {
            if (corridorLen2(mc, x, y, z, -1, 0, 1, 0, false) >= 2) return true;
        }

        return false;
    }

    private int corridorLen(MinecraftWrapper mc, int x, int y, int z, int dx1, int dz1, int dx2, int dz2) {
        int len = 1;
        len += countDir(mc, x, y, z, dx1, dz1, dx2, dz2);
        len += countDir(mc, x, y, z, -dx1, -dz1, -dx2, -dz2);
        return len;
    }

    private int countDir(MinecraftWrapper mc, int x, int y, int z, int dx, int dz, int sdx1, int sdz1) {
        int c = 0;
        int cx = x + dx, cz = z + dz;
        while (c < 8 && canStandIn(mc, cx, y, cz)
            && isSolidColumn(mc, cx + sdx1, y, cz + sdz1)
            && isSolidColumn(mc, cx - sdx1, y, cz - sdz1)) {
            c++;
            cx += dx;
            cz += dz;
        }
        return c;
    }

    private int corridorLen2(MinecraftWrapper mc, int x, int y, int z, int dx1, int dz1, int dx2, int dz2, boolean pairPlus) {
        int len = 1;
        len += countDir2(mc, x, y, z, dx1, dz1, dx2, dz2, pairPlus);
        len += countDir2(mc, x, y, z, -dx1, -dz1, -dx2, -dz2, pairPlus);
        return len;
    }

    private int countDir2(MinecraftWrapper mc, int x, int y, int z, int dx, int dz, int sdx1, int sdz1, boolean pairPlus) {
        int c = 0;
        int ox = pairPlus ? sdx1 : -sdx1;
        int oz = pairPlus ? sdz1 : -sdz1;
        int cx = x + dx, cz = z + dz;
        while (c < 8 && canStandIn(mc, cx, y, cz) && canStandIn(mc, cx + ox, y, cz + oz)
            && isSolidColumn(mc, cx - sdx1, y, cz - sdz1)
            && isSolidColumn(mc, cx + sdx1 + ox, y, cz + sdz1 + oz)) {
            c++;
            cx += dx;
            cz += dz;
        }
        return c;
    }

    private boolean canStandIn(MinecraftWrapper mc, int x, int y, int z) {
        return canWalkOn(mc, x, y - 1, z) && passable(mc, x, y, z) && passable(mc, x, y + 1, z);
    }

    private boolean isSolidColumn(MinecraftWrapper mc, int x, int y, int z) {
        return !passable(mc, x, y, z) || !passable(mc, x, y + 1, z);
    }

    private boolean canWalkOn(MinecraftWrapper mc, int x, int y, int z) {
        BlockState state = mc.getLevel().getBlockState(BP.set(x, y, z));
        if (state.isAir()) return false;
        if (!state.getFluidState().isEmpty()) return false;
        return !state.getCollisionShape(mc.getLevel(), BP).isEmpty();
    }

    private boolean passable(MinecraftWrapper mc, int x, int y, int z) {
        BlockState state = mc.getLevel().getBlockState(BP.set(x, y, z));
        if (state.isAir()) return true;
        if (!state.getFluidState().isEmpty()) return false;
        return state.getCollisionShape(mc.getLevel(), BP).isEmpty();
    }

    public List<BlockPos> getTunnelBlocks() {
        return tunnelBlocks;
    }

    public void onDisable() {
        tunnelBlocks = Collections.emptyList();
        tunnelTick = 0;
    }
}
