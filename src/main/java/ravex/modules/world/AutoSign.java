package ravex.modules.world;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.ScreenUtility;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.SwingUtility;

@Module(name = "AutoSign", category = "World")
public class AutoSign {
    @Parameter(name = "Mode", modes = {"Simple", "Advanced"})
    public String mode = "Simple";
    @Parameter(name = "Line1")
    public String line1 = "";
    @Parameter(name = "Line2")
    public String line2 = "";
    @Parameter(name = "Line3")
    public String line3 = "";
    @Parameter(name = "Line4")
    public String line4 = "";
    @Parameter(name = "AutoPlace", visible = "mode=Advanced")
    public boolean autoPlace = true;
    @Parameter(name = "PlaceMode", modes = {"Crosshair", "Range"}, visible = "mode=Advanced")
    public String placeMode = "Crosshair";
    @Parameter(name = "PlaceDelay", min = 50, max = 2000, step = 50, visible = "mode=Advanced")
    public double placeDelay = 500;
    @Parameter(name = "PlaceRange", min = 1, max = 6, step = 0.5, visible = "mode=Advanced")
    public double placeRange = 4.0;
    @Parameter(name = "CloseDelay", min = 0, max = 1000, step = 50, visible = "mode=Advanced")
    public double closeDelay = 0;
    @Parameter(name = "SwapBack", visible = "mode=Advanced")
    public boolean swapBack = true;

    private long lastPlaceTime;
    private long pendingCloseAt;

    public void onEnable() {
        lastPlaceTime = 0;
        pendingCloseAt = 0;
    }

    public void onDisable() {
        pendingCloseAt = 0;
    }

    public boolean isAdvanced() {
        return "Advanced".equals(mode);
    }

    public void closeSignScreen() {
        if (closeDelay <= 0) {
            var mc = MinecraftWrapper.getWrapper();
            mc.execute(() -> ScreenUtility.closeScreen(mc));
            return;
        }
        pendingCloseAt = System.currentTimeMillis() + (long) closeDelay;
    }

    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        long now = System.currentTimeMillis();
        if (pendingCloseAt > 0 && now >= pendingCloseAt) {
            pendingCloseAt = 0;
            if (mc.getCurrentScreen() instanceof AbstractSignEditScreen) {
                ScreenUtility.closeScreen(mc);
            }
        }
        if (!isAdvanced() || !autoPlace) return;
        var player = mc.getPlayer();
        if (player == null || mc.getLevel() == null || mc.getCurrentScreen() != null) return;
        if (now - lastPlaceTime < placeDelay) return;
        if (mc.getGameMode() == null) return;
        var useHit = findPlaceHit(mc, player);
        if (useHit == null) return;
        int signSlot = findSignSlot(player);
        if (signSlot == -1) return;
        int prevSlot = InventoryUtility.getSelectedSlot(player);
        if (signSlot != prevSlot) {
            InventoryUtility.selectSlot(player, signSlot);
        }
        mc.getGameMode().useItemOn(player, InteractionHand.MAIN_HAND, useHit);
        SwingUtility.swing(player, InteractionHand.MAIN_HAND);
        if (swapBack && signSlot != prevSlot) {
            InventoryUtility.selectSlot(player, prevSlot);
        }
        lastPlaceTime = now;
    }

    private BlockHitResult findPlaceHit(MinecraftWrapper mc, LocalPlayer player) {
        if ("Range".equals(placeMode)) {
            BlockPos target = findGroundTarget(mc, player);
            if (target == null) return null;
            var hit = BlockUtility.findPlaceTarget(mc, target);
            if (hit == null || hit.getLocation().distanceTo(player.getEyePosition()) > placeRange) return null;
            return hit;
        }
        var hit = mc.getHitResult();
        if (!(hit instanceof BlockHitResult blockHit)) return null;
        if (hit.getLocation().distanceTo(player.getEyePosition()) > placeRange) return null;
        BlockPos placePos = blockHit.getBlockPos().relative(blockHit.getDirection());
        if (!mc.getLevel().getBlockState(placePos).isAir()) return null;
        return new BlockHitResult(hit.getLocation(), blockHit.getDirection(), blockHit.getBlockPos(), false);
    }

    private BlockPos findGroundTarget(MinecraftWrapper mc, LocalPlayer player) {
        var level = mc.getLevel();
        Vec3 eye = player.getEyePosition();
        BlockPos feet = player.blockPosition();
        BlockPos base = feet.above();
        double bestDist = placeRange * placeRange;
        BlockPos best = null;
        int r = (int) Math.ceil(placeRange);
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos pos = base.offset(dx, dy, dz);
                    if (pos.equals(feet) || pos.equals(feet.above())) continue;
                    if (!level.getBlockState(pos).isAir()) continue;
                    BlockPos below = pos.below();
                    if (!level.getBlockState(below).isCollisionShapeFullBlock(level, below)) continue;
                    double dist = eye.distanceToSqr(Vec3.atCenterOf(pos));
                    if (dist > bestDist) continue;
                    bestDist = dist;
                    best = pos;
                }
            }
        }
        return best;
    }

    private int findSignSlot(LocalPlayer player) {
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(player, i);
            if (stack == null || stack.isEmpty()) continue;
            var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id != null && id.getPath().endsWith("_sign")) return i;
        }
        return -1;
    }
}
