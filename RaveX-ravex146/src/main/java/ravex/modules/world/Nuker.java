package ravex.modules.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ActionParameter;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.misc.NativeLoader;

import java.util.ArrayList;
import java.util.List;

public class Nuker extends Module {
    public static final Nuker INSTANCE = new Nuker();

    public final NumberParameter range = new NumberParameter("Range", 5.0, 1.0, 10.0, 0.5);
    public final ModeParameter mode = new ModeParameter("Mode", "Sphere", List.of("Sphere", "Cube"));
    public final NumberParameter delay = new NumberParameter("Delay (ms)", 200, 50, 1000, 50);
    public final BooleanParameter autoDisable = new BooleanParameter("Auto Disable", false);
    public final BooleanParameter render = new BooleanParameter("Render", true);
    public final ColorParameter color = new ColorParameter("Color", 0x3FFF4444);
    public final ActionParameter blocks = new ActionParameter("Blocks", () -> {
        Minecraft.getInstance().setScreen(
            new ravex.gui.blockbrowser.NukerBlockBrowserScreen(Minecraft.getInstance().screen)
        );
    });

    public static BlockPos currentTarget = null;

    private long lastBreakTime = 0;
    private BlockPos currentMiningTarget = null;

    private static boolean nativeLibLoaded = false;
    static {
        try {
            nativeLibLoaded = NativeLoader.loadLibrary("ravex_nuker");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("[Nuker JNI] Failed to load native library: " + e.getMessage());
        }
    }

    public static native int[] nativeFindBlocks(
        double px, double py, double pz,
        double range,
        int mode,
        int[] bx, int[] by, int[] bz,
        int blockCount
    );

    private Nuker() {
        super("Nuker", Category.WORLD);
        addParameter(range);
        addParameter(mode);
        addParameter(delay);
        addParameter(autoDisable);
        addParameter(render);
        addParameter(color);
        addParameter(blocks);
    }

    @Override
    public void saveExtra(JsonObject obj) {
        JsonArray arr = new JsonArray();
        for (Identifier id : NukerData.INSTANCE.getSelectedBlocks()) {
            arr.add(id.toString());
        }
        obj.add("selectedBlocks", arr);
    }

    @Override
    public void loadExtra(JsonObject obj) {
        if (!obj.has("selectedBlocks")) return;
        NukerData.INSTANCE.clear();
        JsonArray arr = obj.getAsJsonArray("selectedBlocks");
        for (int i = 0; i < arr.size(); i++) {
            Identifier id = Identifier.tryParse(arr.get(i).getAsString());
            NukerData.INSTANCE.select(id);
        }
    }

    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (currentMiningTarget != null && mc.gameMode != null) {
            mc.gameMode.stopDestroyBlock();
        }
        currentMiningTarget = null;
        currentTarget = null;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

        long now = System.currentTimeMillis();

        if (currentMiningTarget != null) {
            BlockState state = mc.level.getBlockState(currentMiningTarget);
            if (state.isAir()) {
                currentMiningTarget = null;
                currentTarget = null;
            } else {
                Direction dir = getDirection(mc.player.getEyePosition(), currentMiningTarget);
                mc.gameMode.continueDestroyBlock(currentMiningTarget, dir);
                mc.player.swing(InteractionHand.MAIN_HAND);
                lastBreakTime = now;
                return;
            }
        }

        if (now - lastBreakTime < delay.getValue()) return;

        double r = range.getValue();
        BlockPos playerPos = mc.player.blockPosition();
        int minX = (int) Math.floor(playerPos.getX() - r);
        int maxX = (int) Math.ceil(playerPos.getX() + r);
        int minY = (int) Math.max(mc.level.getMinY(), Math.floor(playerPos.getY() - r));
        int maxY = (int) Math.min(mc.level.getMaxY(), Math.ceil(playerPos.getY() + r));
        int minZ = (int) Math.floor(playerPos.getZ() - r);
        int maxZ = (int) Math.ceil(playerPos.getZ() + r);

        List<BlockPos> candidates = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = mc.level.getBlockState(pos);
                    if (state.isAir() || state.getDestroySpeed(mc.level, pos) < 0) continue;

                    Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                    if (NukerData.INSTANCE.isSelected(id)) {
                        candidates.add(pos);
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            currentTarget = null;
            if (autoDisable.getValue()) setEnabled(false);
            return;
        }

        BlockPos target = null;

        if (nativeLibLoaded) {
            try {
                int cnt = candidates.size();
                int[] bx = new int[cnt];
                int[] by = new int[cnt];
                int[] bz = new int[cnt];
                for (int i = 0; i < cnt; i++) {
                    bx[i] = candidates.get(i).getX();
                    by[i] = candidates.get(i).getY();
                    bz[i] = candidates.get(i).getZ();
                }

                int modeVal = "Sphere".equals(mode.getValue()) ? 0 : 1;
                Vec3 eye = mc.player.getEyePosition();
                int[] result = nativeFindBlocks(
                    eye.x, eye.y, eye.z,
                    r, modeVal,
                    bx, by, bz, cnt
                );

                if (result.length >= 3) {
                    target = new BlockPos(result[0], result[1], result[2]);
                }
            } catch (Exception e) {
                target = null;
            }
        }

        if (target == null) {
            target = fallbackFindTarget(candidates, mc);
        }

        if (target != null) {
            if (!target.equals(currentMiningTarget)) {
                if (currentMiningTarget != null) {
                    mc.gameMode.stopDestroyBlock();
                }
            }
            currentMiningTarget = target;
            currentTarget = target;
            Direction dir = getDirection(mc.player.getEyePosition(), target);
            mc.gameMode.startDestroyBlock(target, dir);
            mc.player.swing(InteractionHand.MAIN_HAND);
            try {
                NoGhostBlocks.markMined(target);
            } catch (Exception e) {
                // Ignore errors from NoGhostBlocks
            }
            lastBreakTime = now;
        }
    }

    private BlockPos fallbackFindTarget(List<BlockPos> candidates, Minecraft mc) {
        boolean sphere = "Sphere".equals(mode.getValue());
        Vec3 eye = mc.player.getEyePosition();
        double rSq = range.getValue() * range.getValue();

        BlockPos closest = null;
        double closestDist = Double.MAX_VALUE;

        for (BlockPos pos : candidates) {
            if (sphere) {
                Vec3 center = Vec3.atCenterOf(pos);
                double distSq = eye.distanceToSqr(center);
                if (distSq > rSq) continue;
                if (distSq < closestDist) {
                    closestDist = distSq;
                    closest = pos;
                }
            } else {
                double dist = eye.distanceTo(Vec3.atCenterOf(pos));
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = pos;
                }
            }
        }

        return closest;
    }

    public static Direction getDirection(Vec3 eye, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        double dx = eye.x - center.x;
        double dy = eye.y - pos.getY() - 0.5;
        double dz = eye.z - center.z;

        double absX = Math.abs(dx);
        double absY = Math.abs(dy);
        double absZ = Math.abs(dz);

        if (absY <= absX && absY <= absZ) {
            if (absX >= absZ) {
                return dx > 0 ? Direction.EAST : Direction.WEST;
            } else {
                return dz > 0 ? Direction.SOUTH : Direction.NORTH;
            }
        } else if (absX <= absY && absX <= absZ) {
            if (absY >= absZ) {
                return dy > 0 ? Direction.DOWN : Direction.UP;
            } else {
                return dz > 0 ? Direction.SOUTH : Direction.NORTH;
            }
        } else {
            if (absY >= absX) {
                return dy > 0 ? Direction.DOWN : Direction.UP;
            } else {
                return dx > 0 ? Direction.EAST : Direction.WEST;
            }
        }
    }
}
