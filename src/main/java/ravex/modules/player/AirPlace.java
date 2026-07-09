package ravex.modules.player;
import ravex.manager.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.SwingUtility;
import ravex.utility.render.animate.EasingAnimation;
import ravex.utility.render.animate.SlideAnimation;
public class AirPlace extends Module {
    public static Vec3 highlightPos = null;
    public static float renderAlpha = 0.0f;
    public static double renderSize = 0.0;
    public static float renderR = 0.3f;
    public static float renderG = 0.7f;
    public static float renderB = 1.0f;
    public final BooleanParameter grimStrict = new BooleanParameter("GrimStrict", false);
    public final BooleanParameter render = new BooleanParameter("Render", true);
    public final BooleanParameter animate = new BooleanParameter("Animate", true);
    public final ColorParameter highlightColor = new ColorParameter("HighlightColor", 0xFF55AAFF);
    private final EasingAnimation fadeAnim = new EasingAnimation();
    private final EasingAnimation sizeAnim = new EasingAnimation();
    private final SlideAnimation slideAnim = new SlideAnimation();
    public BlockPos currentTarget = null;
    private long lastPlaceTime = 0;
    private boolean grimBroken = false;

    @Override
    protected void onEnable() {
        highlightPos = null;
        renderAlpha = 0.0f;
        renderSize = 0.0;
        currentTarget = null;
        grimBroken = false;
        fadeAnim.reset();
        sizeAnim.reset();
        slideAnim.reset();
    }
    @Override
    protected void onDisable() {
        highlightPos = null;
        renderAlpha = 0.0f;
        renderSize = 0.0;
        currentTarget = null;
        grimBroken = false;
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            highlightPos = null;
            return;
        }
        boolean mainHolding = InventoryUtility.isHoldingBlock(mc.player);
        var off = mc.player.getOffhandItem();
        boolean offHolding = !off.isEmpty() && off.getItem() instanceof BlockItem;
        var hand = mainHolding ? InteractionHand.MAIN_HAND : (offHolding ? InteractionHand.OFF_HAND : null);
        if (hand == null) {
            currentTarget = null;
            renderAlpha = fadeAnim.updateFloat(false, 0.25f);
            renderSize = sizeAnim.update(false, 0.15);
            if (renderAlpha <= 0.01f) highlightPos = null;
            return;
        }
        double dist = 4.5;
        var hit = mc.player.pick(dist, 1.0F, false);
        BlockPos targetPos;
        BlockPos neighbor;
        Direction placeFace;
        if (hit != null && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            var bhr = (BlockHitResult) hit;
            neighbor = bhr.getBlockPos();
            placeFace = bhr.getDirection();
            targetPos = neighbor.relative(placeFace);
            grimBroken = false;
        } else {
            Vec3 eye = mc.player.getEyePosition(1.0F);
            Vec3 look = mc.player.getViewVector(1.0F);
            Vec3 target = eye.add(look.x * dist, look.y * dist, look.z * dist);
            targetPos = BlockUtility.containing(target.x, target.y, target.z);
            neighbor = targetPos;
            placeFace = Direction.UP;
            for (Direction face : Direction.values()) {
                BlockPos side = targetPos.relative(face);
                if (BlockUtility.isAir(mc.level, side)) continue;
                neighbor = side;
                placeFace = face.getOpposite();
                break;
            }
        }
        currentTarget = targetPos;
        if (render.getValue()) {
            int hc = highlightColor.getValue();
            renderR = ((hc >> 16) & 0xFF) / 255.0f;
            renderG = ((hc >> 8) & 0xFF) / 255.0f;
            renderB = (hc & 0xFF) / 255.0f;
        }
        if (mc.options.keyUse.isDown()) {
            long now = System.currentTimeMillis();
            if (now - lastPlaceTime > 200) {
                if (grimStrict.getValue()) {
                    neighbor = targetPos;
                    for (Direction face : Direction.values()) {
                        BlockPos side = targetPos.relative(face);
                        if (BlockUtility.isAir(mc.level, side)) continue;
                        neighbor = side;
                        placeFace = face.getOpposite();
                        break;
                    }
                    if (!grimBroken && !BlockUtility.isAir(mc.level, neighbor)) {
                        Vec3 hitVec = Vec3.atCenterOf(neighbor).add(
                            new Vec3(placeFace.getStepX(), placeFace.getStepY(), placeFace.getStepZ()).scale(0.5)
                        );
                        BlockHitResult blockHit = new BlockHitResult(hitVec, placeFace, neighbor, false);
                        BlockUtility.useItemOn(mc, blockHit, hand);
                        SwingUtility.swing(mc.player, hand);
                    } else {
                        BlockPos breakPos = targetPos.relative(placeFace.getOpposite());
                        BlockUtility.grimAirPlaceDesync(mc, targetPos, breakPos, hand);
                        grimBroken = true;
                    }
                } else {
                    Vec3 hitVec = Vec3.atCenterOf(neighbor).add(
                        new Vec3(placeFace.getStepX(), placeFace.getStepY(), placeFace.getStepZ()).scale(0.5)
                    );
                    BlockHitResult blockHit = new BlockHitResult(hitVec, placeFace, neighbor, false);
                    BlockUtility.useItemOn(mc, blockHit, hand);
                    SwingUtility.swing(mc.player, hand);
                }
                lastPlaceTime = now;
            }
        } else {
            grimBroken = false;
        }
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(AirPlace.class);
    }
    public static AirPlace itz() {
        return ModuleManager.get(AirPlace.class);
    }
}
