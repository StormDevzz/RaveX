package ravex.modules.player;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.SwingUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.render.animate.EasingAnimation;
import ravex.utility.render.animate.SlideAnimation;
<<<<<<< HEAD
public class AirPlace extends Module {
=======
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.SwingUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
public class AirPlace extends Module {
    public static final AirPlace INSTANCE = new AirPlace();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public static Vec3 highlightPos = null;
    public static float renderAlpha = 0.0f;
    public static double renderSize = 0.0;
    public static float renderR = 0.3f;
    public static float renderG = 0.7f;
    public static float renderB = 1.0f;
<<<<<<< HEAD
<<<<<<< HEAD
    public final BooleanParameter grimStrict = new BooleanParameter("GrimStrict", false);
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
=======
    public final ModeParameter mode = new ModeParameter("Mode", "Vanilla", java.util.List.of("Vanilla", "Grim"));
>>>>>>> 0ab37177398daa0e9880b2ec0d3ee76a2dbed416
    public final BooleanParameter render = new BooleanParameter("Render", true);
    public final BooleanParameter animate = new BooleanParameter("Animate", true);
    public final ColorParameter highlightColor = new ColorParameter("HighlightColor", 0xFF55AAFF);
    private final EasingAnimation fadeAnim = new EasingAnimation();
    private final EasingAnimation sizeAnim = new EasingAnimation();
    private final SlideAnimation slideAnim = new SlideAnimation();
    public BlockPos currentTarget = null;
    private long lastPlaceTime = 0;
<<<<<<< HEAD
<<<<<<< HEAD
    private boolean grimBroken = false;
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
=======
>>>>>>> 0ab37177398daa0e9880b2ec0d3ee76a2dbed416

    @Override
    protected void onEnable() {
        highlightPos = null;
        renderAlpha = 0.0f;
        renderSize = 0.0;
        currentTarget = null;
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
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            highlightPos = null;
            return;
        }
        boolean mainHolding = InventoryUtility.isHoldingBlock(mc.player);
<<<<<<< HEAD
        var off = mc.player.getOffhandItem();
        boolean offHolding = !off.isEmpty() && off.getItem() instanceof BlockItem;
        var hand = mainHolding ? InteractionHand.MAIN_HAND : (offHolding ? InteractionHand.OFF_HAND : null);
=======
        ItemStack off = mc.player.getOffhandItem();
        boolean offHolding = !off.isEmpty() && off.getItem() instanceof net.minecraft.world.item.BlockItem;
        InteractionHand hand = mainHolding ? InteractionHand.MAIN_HAND : (offHolding ? InteractionHand.OFF_HAND : null);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (hand == null) {
            currentTarget = null;
            renderAlpha = fadeAnim.updateFloat(false, 0.25f);
            renderSize = sizeAnim.update(false, 0.15);
            if (renderAlpha <= 0.01f) highlightPos = null;
            return;
        }
        double dist = 4.5;
<<<<<<< HEAD
        var hit = mc.player.pick(dist, 1.0F, false);
        BlockPos targetPos;
        BlockPos neighbor;
        Direction placeFace;
=======
        net.minecraft.world.phys.HitResult hit = mc.player.pick(dist, 1.0F, false);
        BlockPos targetPos;
        BlockPos neighbor;
        net.minecraft.core.Direction placeFace;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (hit != null && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            var bhr = (BlockHitResult) hit;
            neighbor = bhr.getBlockPos();
            placeFace = bhr.getDirection();
            targetPos = neighbor.relative(placeFace);
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
<<<<<<< HEAD
<<<<<<< HEAD
                if (grimStrict.getValue()) {
=======
                if (mode.getValue().equals("Grim")) {
>>>>>>> 0ab37177398daa0e9880b2ec0d3ee76a2dbed416
                    neighbor = targetPos;
                    placeFace = Direction.UP;
                    for (Direction face : Direction.values()) {
                        BlockPos side = targetPos.relative(face);
                        if (BlockUtility.isAir(mc.level, side)) continue;
                        neighbor = side;
                        placeFace = face.getOpposite();
                        break;
                    }
                    if (!BlockUtility.isAir(mc.level, neighbor)) {
                        double maxReach = 4.5;
                        Vec3 center = Vec3.atCenterOf(neighbor);
                        if (mc.player.getEyePosition().distanceTo(center) <= maxReach) {
                            Vec3 hitVec = center.add(
                                new Vec3(placeFace.getStepX(), placeFace.getStepY(), placeFace.getStepZ()).scale(0.5)
                            );
                            float[] angles = RotationUtility.anglesTo(mc.player.getEyePosition(), hitVec);
                            var conn = mc.getConnection();
                            if (conn != null) {
                                conn.send(new ServerboundMovePlayerPacket.PosRot(
                                    mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                                    angles[0], angles[1], mc.player.onGround(), mc.player.horizontalCollision
                                ));
                            }
                            BlockHitResult blockHit = new BlockHitResult(hitVec, placeFace, neighbor, false);
                            BlockUtility.useItemOn(mc, blockHit, hand);
                            if (conn != null) {
                                conn.send(new ServerboundMovePlayerPacket.PosRot(
                                    mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                                    mc.player.getYRot(), mc.player.getXRot(), mc.player.onGround(), mc.player.horizontalCollision
                                ));
                            }
                            SwingUtility.swing(mc.player, hand);
                        }
                    }
                } else {
                    Vec3 hitVec = Vec3.atCenterOf(neighbor).add(
                        new Vec3(placeFace.getStepX(), placeFace.getStepY(), placeFace.getStepZ()).scale(0.5)
                    );
                    BlockHitResult blockHit = new BlockHitResult(hitVec, placeFace, neighbor, false);
                    BlockUtility.useItemOn(mc, blockHit, hand);
                    SwingUtility.swing(mc.player, hand);
                }
=======
                Vec3 hitVec = Vec3.atCenterOf(neighbor).add(
                    new Vec3(placeFace.getStepX(), placeFace.getStepY(), placeFace.getStepZ()).scale(0.5)
                );
                net.minecraft.world.phys.BlockHitResult blockHit = new net.minecraft.world.phys.BlockHitResult(
                    hitVec, placeFace, neighbor, false
                );
                mc.gameMode.useItemOn(mc.player, hand, blockHit);
                SwingUtility.swing(mc.player, hand);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                lastPlaceTime = now;
            }
        }
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(AirPlace.class);
    }
    public static AirPlace itz() {
        return ModuleManager.get(AirPlace.class);
    }
}
