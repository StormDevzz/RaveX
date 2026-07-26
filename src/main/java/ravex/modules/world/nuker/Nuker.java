package ravex.modules.world.nuker;

import ravex.modules.annotations.ModuleInfo;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import ravex.utility.misc.block.BlockUtility;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import ravex.utility.player.SwingUtility;
import net.minecraft.world.level.block.state.BlockState;
import ravex.utility.misc.PhysicUtility;

import ravex.parameter.ActionParameter;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.modules.world.GhostBlocks;
import ravex.utility.nativelib.NativeLibraryUtility;
import java.util.ArrayList;
import java.util.List;
@ModuleInfo(name = "Nuker", category = "World")
public class Nuker extends ravex.modules.Module {
public final NumberParameter range = new NumberParameter("Range", 5.0, 1.0, 10.0, 0.5);
    public final ModeParameter mode = new ModeParameter("Mode", "Sphere", List.of("Sphere", "Cube"));
    public final NumberParameter delay = new NumberParameter("Delay", 200, 50, 1000, 50);
    public final BooleanParameter autoDisable = new BooleanParameter("AutoDisable", false);
    public final BooleanParameter render = new BooleanParameter("Render", true);
    public final ColorParameter color = new ColorParameter("Color", 0x3FFF4444);
    public final ActionParameter blocks = new ActionParameter("net.minecraft.world.level.block.Blocks", () -> {
        Minecraft.getInstance().setScreen(
            ravex.gui.browser.BlockBrowserScreen.forNuker(Minecraft.getInstance().screen)
        );
    });
    public static net.minecraft.core.BlockPos currentTarget = null;
    private long lastBreakTime = 0;
    private net.minecraft.core.BlockPos currentMiningTarget = null;
    private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_nuker");
    static {
        NATIVE.load();
    }
    public static native int[] nativeFindBlocks(
        double px, double py, double pz,
        double range,
        int mode,
        int[] bx, int[] by, int[] bz,
        int blockCount
    );
    public void saveExtra(JsonObject obj) {
        JsonArray arr = new JsonArray();
        for (Identifier id : NukerData.INSTANCE.getSelectedBlocks()) {
            arr.add(id.toString());
        }
        obj.add("selectedBlocks", arr);
    }
    public void loadExtra(JsonObject obj) {
        if (!obj.has("selectedBlocks")) return;
        NukerData.INSTANCE.clear();
        JsonArray arr = obj.getAsJsonArray("selectedBlocks");
        for (int i = 0; i < arr.size(); i++) {
            Identifier id = Identifier.tryParse(arr.get(i).getAsString());
            NukerData.INSTANCE.select(id);
        }
    }
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (currentMiningTarget != null && mc.gameMode != null) {
            mc.gameMode.stopDestroyBlock();
        }
        currentMiningTarget = null;
        currentTarget = null;
    }
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
                net.minecraft.core.Direction dir = getDirection(mc.player.getEyePosition(), currentMiningTarget);
                mc.gameMode.continueDestroyBlock(currentMiningTarget, dir);
                mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                lastBreakTime = now;
                return;
            }
        }
        if (now - lastBreakTime < delay.getValue()) return;
        double r = range.getValue();
        net.minecraft.core.BlockPos playerPos = mc.player.blockPosition();
        int minX = (int) Math.floor(playerPos.getX() - r);
        int maxX = (int) Math.ceil(playerPos.getX() + r);
        int minY = (int) Math.max(mc.level.getMinY(), Math.floor(playerPos.getY() - r));
        int maxY = (int) Math.min(mc.level.getMaxY(), Math.ceil(playerPos.getY() + r));
        int minZ = (int) Math.floor(playerPos.getZ() - r);
        int maxZ = (int) Math.ceil(playerPos.getZ() + r);
        List<net.minecraft.core.BlockPos> candidates = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x, y, z);
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
            if (autoDisable.getValue()) enabled = false;
            return;
        }
        net.minecraft.core.BlockPos target = null;
        if (NATIVE.isLoaded()) {
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
                net.minecraft.world.phys.Vec3 eye = mc.player.getEyePosition();
                int[] result = nativeFindBlocks(
                    eye.x, eye.y, eye.z,
                    r, modeVal,
                    bx, by, bz, cnt
                );
                if (result.length >= 3) {
                    target = new net.minecraft.core.BlockPos(result[0], result[1], result[2]);
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
            net.minecraft.core.Direction dir = getDirection(mc.player.getEyePosition(), target);
            mc.gameMode.startDestroyBlock(target, dir);
            mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            GhostBlocks.markMined(target);
            lastBreakTime = now;
        }
    }
    private net.minecraft.core.BlockPos fallbackFindTarget(List<net.minecraft.core.BlockPos> candidates, Minecraft mc) {
        boolean sphere = "Sphere".equals(mode.getValue());
        net.minecraft.world.phys.Vec3 eye = mc.player.getEyePosition();
        double rSq = range.getValue() * range.getValue();
        net.minecraft.core.BlockPos closest = null;
        double closestDist = Double.MAX_VALUE;
        for (net.minecraft.core.BlockPos pos : candidates) {
            if (sphere) {
                net.minecraft.world.phys.Vec3 center = net.minecraft.world.phys.Vec3.atCenterOf(pos);
                double distSq = eye.distanceToSqr(center);
                if (distSq > rSq) continue;
                if (distSq < closestDist) {
                    closestDist = distSq;
                    closest = pos;
                }
            } else {
                double dist = eye.distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(pos));
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = pos;
                }
            }
        }
        return closest;
    }
    public static net.minecraft.core.Direction getDirection(net.minecraft.world.phys.Vec3 eye, net.minecraft.core.BlockPos pos) {
        net.minecraft.world.phys.Vec3 center = net.minecraft.world.phys.Vec3.atCenterOf(pos);
        double dx = eye.x - center.x;
        double dy = eye.y - pos.getY() - 0.5;
        double dz = eye.z - center.z;
        double absX = Math.abs(dx);
        double absY = Math.abs(dy);
        double absZ = Math.abs(dz);
        if (absY <= absX && absY <= absZ) {
            if (absX >= absZ) {
                return dx > 0 ? net.minecraft.core.Direction.EAST : net.minecraft.core.Direction.WEST;
            } else {
                return dz > 0 ? net.minecraft.core.Direction.SOUTH : net.minecraft.core.Direction.NORTH;
            }
        } else if (absX <= absY && absX <= absZ) {
            if (absY >= absZ) {
                return dy > 0 ? net.minecraft.core.Direction.DOWN : net.minecraft.core.Direction.UP;
            } else {
                return dz > 0 ? net.minecraft.core.Direction.SOUTH : net.minecraft.core.Direction.NORTH;
            }
        } else {
            if (absY >= absX) {
                return dy > 0 ? net.minecraft.core.Direction.DOWN : net.minecraft.core.Direction.UP;
            } else {
                return dx > 0 ? net.minecraft.core.Direction.EAST : net.minecraft.core.Direction.WEST;
            }
        }
    }
    public static Nuker itz() {
        return ravex.manager.ModuleManager.delegate(Nuker.class);
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