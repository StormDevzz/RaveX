package ravex.modules.world.nuker;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.parameter.ActionParameter;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.misc.PhysicUtility;
import ravex.utility.nativelib.NativeLibraryUtility;
import ravex.utility.player.SwingUtility;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;




@Module(name = "Nuker", category = "World")
public class Nuker {
    @Parameter(name = "Range", min = 1.0, max = 10.0, step = 0.5)
    public double range = 5.0;
    @Parameter(name = "Mode", modes = {"Sphere", "Cube"})
    public String mode = "Sphere";
    @Parameter(name = "List", modes = {"Whitelist", "Blacklist"})
    public String listMode = "Whitelist";
    @Parameter(name = "Delay", min = 50, max = 1000, step = 50)
    public double delay = 200;
    @Parameter(name = "AutoDisable")
    public boolean autoDisable = false;
    @Parameter(name = "Render")
    public boolean render = true;
    @Parameter(name = "Color", color = true, visible = "render")
    public int color = 0x3F40A9F8;
    public final ActionParameter blocks = new ActionParameter("Blocks", () -> {
        MinecraftWrapper.getWrapper().setScreen(
            ravex.gui.browser.            BlockBrowserScreen.forNuker(MinecraftWrapper.getWrapper().getCurrentScreen())
        );
    });
    public static net.minecraft.core.BlockPos currentTarget = null;
    public static final ravex.utility.player.rotation.SilentRotationUtility silentRotation =
        new ravex.utility.player.rotation.SilentRotationUtility();
    private net.minecraft.core.BlockPos aimedTarget = null;
    private long lastBreakTime = 0;
    private static net.minecraft.core.BlockPos currentMiningTarget = null;
    private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_nuker");
    public static native int[] nativeFindBlocks(
        double px, double py, double pz,
        double range,
        int mode,
        int[] bx, int[] by, int[] bz,
        int blockCount
    );
    public void saveExtra(JsonObject obj) {
        JsonArray arr = new JsonArray();
        for (String id : NukerData.INSTANCE.getSelectedBlocks()) {
            arr.add(id);
        }
        obj.add("selectedBlocks", arr);
    }
    public void loadExtra(JsonObject obj) {
        if (!obj.has("selectedBlocks")) return;
        NukerData.INSTANCE.clear();
        JsonArray arr = obj.getAsJsonArray("selectedBlocks");
        for (int i = 0; i < arr.size(); i++) {
            String id = arr.get(i).getAsString();
            if (id.indexOf(':') > 0) NukerData.INSTANCE.select(id);
        }
    }
    public void onDisable() {
        var mc = MinecraftWrapper.getWrapper();
        currentMiningTarget = null;
        currentTarget = null;
        aimedTarget = null;
        silentRotation.hasRotation = false;
        if (mc.getGameMode() != null) {
            mc.getGameMode().stopDestroyBlock();
        }
    }
    public static boolean ownsMining() {
        if (!Modules.enabled(Nuker.class) || currentMiningTarget == null) return false;
        var mc = MinecraftWrapper.getWrapper();
        return mc.getLevel() != null && !mc.getLevel().getBlockState(currentMiningTarget).isAir();
    }
    public static boolean blocksVanillaMining(net.minecraft.core.BlockPos pos) {
        return ownsMining() && !pos.equals(currentMiningTarget);
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null || mc.getGameMode() == null) return;
        long now = System.currentTimeMillis();
        if (currentMiningTarget != null) {
            net.minecraft.world.level.block.state.BlockState state = mc.getLevel().getBlockState(currentMiningTarget);
            if (state.isAir()) {
                currentMiningTarget = null;
                currentTarget = null;
            } else {
                net.minecraft.core.Direction dir = getDirection(mc.getPlayer().getEyePosition(), currentMiningTarget);
                silentRotation.setAnglesTo(mc, PhysicUtility.centerOf(currentMiningTarget));
                mc.getGameMode().continueDestroyBlock(currentMiningTarget, dir);
                SwingUtility.swing(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND);
                lastBreakTime = now;
                return;
            }
        }
        if (now - lastBreakTime < delay) return;
        double r = range;
        net.minecraft.core.BlockPos playerPos = mc.getPlayer().blockPosition();
        int minX = (int) Math.floor(playerPos.getX() - r);
        int maxX = (int) Math.ceil(playerPos.getX() + r);
        int minY = (int) Math.max(mc.getLevel().getMinY(), Math.floor(playerPos.getY() - r));
        int maxY = (int) Math.min(mc.getLevel().getMaxY(), Math.ceil(playerPos.getY() + r));
        int minZ = (int) Math.floor(playerPos.getZ() - r);
        int maxZ = (int) Math.ceil(playerPos.getZ() + r);
        List<net.minecraft.core.BlockPos> candidates = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x, y, z);
                    net.minecraft.world.level.block.state.BlockState state = mc.getLevel().getBlockState(pos);
                    if (state.isAir() || state.getDestroySpeed(mc.getLevel(), pos) < 0) continue;
                    var blockId = BlockUtility.getIdentifier(state.getBlock());
                    boolean selected = blockId != null && NukerData.INSTANCE.isSelected(blockId.toString());
                    // whitelist breaks the list, blacklist the rest
                    if ("Whitelist".equals(listMode) ? selected : !selected) {
                        candidates.add(pos);
                    }
                }
            }
        }
        if (candidates.isEmpty()) {
            currentTarget = null;
            silentRotation.hasRotation = false;
            if (autoDisable) Modules.setEnabled(Nuker.class, false);
            return;
        }
        double reach = interactionReach(mc, range);
        boolean sphere = "Sphere".equals(mode);
        double limit = sphere ? Math.min(reach, range) : reach;
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
                int modeVal = sphere ? 0 : 1;
                net.minecraft.world.phys.Vec3 eye = mc.getPlayer().getEyePosition();
                int[] result = nativeFindBlocks(
                    eye.x, eye.y, eye.z,
                    limit, modeVal,
                    bx, by, bz, cnt
                );
                if (result != null && result.length >= 3) {
                    target = new net.minecraft.core.BlockPos(result[0], result[1], result[2]);
                }
            } catch (Throwable ignored) {
                target = null;
            }
        }
        if (target == null) {
            target = fallbackFindTarget(candidates, mc, limit, sphere);
        }
        if (target != null) {
            if (!target.equals(aimedTarget)) {
                // rotation already sent, keep breaking the old aim
                boolean keepAimed = aimedTarget != null
                    && candidates.contains(aimedTarget)
                    && targetDistSq(mc.getPlayer().getEyePosition(), aimedTarget, sphere) <= limit * limit;
                if (keepAimed) {
                    target = aimedTarget;
                } else {
                    aimedTarget = target;
                    silentRotation.setAnglesTo(mc, PhysicUtility.centerOf(target));
                    return;
                }
            }
            currentMiningTarget = target;
            currentTarget = target;
            net.minecraft.core.Direction dir = getDirection(mc.getPlayer().getEyePosition(), target);
            mc.getGameMode().startDestroyBlock(target, dir);
            SwingUtility.swing(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND);
            lastBreakTime = now;
        }
    }
    private static double interactionReach(MinecraftWrapper mc, double range) {
        double reach = 4.5;
        try {
            reach = mc.getPlayer().getAttributeValue(
                net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE);
        } catch (Throwable ignored) {
        }
        return Math.max(1.0, Math.min(range, reach - 0.01));
    }
    private net.minecraft.core.BlockPos fallbackFindTarget(List<net.minecraft.core.BlockPos> candidates,
                                                           MinecraftWrapper mc, double limit, boolean sphere) {
        net.minecraft.world.phys.Vec3 eye = mc.getPlayer().getEyePosition();
        double rSq = limit * limit;
        net.minecraft.core.BlockPos closest = null;
        double closestDist = Double.MAX_VALUE;
        for (net.minecraft.core.BlockPos pos : candidates) {
            double distSq = targetDistSq(eye, pos, sphere);
            if (distSq > rSq) continue;
            if (distSq < closestDist) {
                closestDist = distSq;
                closest = pos;
            }
        }
        return closest;
    }
    private static double targetDistSq(net.minecraft.world.phys.Vec3 eye,
                                       net.minecraft.core.BlockPos pos, boolean sphere) {
        return sphere ? eye.distanceToSqr(PhysicUtility.centerOf(pos)) : distToBoxSq(eye, pos);
    }
    private static double distToBoxSq(net.minecraft.world.phys.Vec3 eye, net.minecraft.core.BlockPos pos) {
        double dx = Math.max(Math.max(pos.getX() - eye.x, 0.0), eye.x - pos.getX() - 1.0);
        double dy = Math.max(Math.max(pos.getY() - eye.y, 0.0), eye.y - pos.getY() - 1.0);
        double dz = Math.max(Math.max(pos.getZ() - eye.z, 0.0), eye.z - pos.getZ() - 1.0);
        return dx * dx + dy * dy + dz * dz;
    }
    public static net.minecraft.core.Direction getDirection(net.minecraft.world.phys.Vec3 eye, net.minecraft.core.BlockPos pos) {
        net.minecraft.world.phys.Vec3 center = PhysicUtility.centerOf(pos);
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
                return dy > 0 ? net.minecraft.core.Direction.UP : net.minecraft.core.Direction.DOWN;
            } else {
                return dz > 0 ? net.minecraft.core.Direction.SOUTH : net.minecraft.core.Direction.NORTH;
            }
        } else {
            if (absY >= absX) {
                return dy > 0 ? net.minecraft.core.Direction.UP : net.minecraft.core.Direction.DOWN;
            } else {
                return dx > 0 ? net.minecraft.core.Direction.EAST : net.minecraft.core.Direction.WEST;
            }
        }
    }



}
