package ravex.modules.player;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.misc.PhysicUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.SwingUtility;
import ravex.utility.render.animate.EasingAnimationUtility;
import ravex.utility.render.animate.SlideAnimationUtility;
import net.minecraft.world.item.BlockItem;
import ravex.mcwrapper.MinecraftWrapper;




@ModuleInfo(name = "AirPlace", category = "net.minecraft.world.entity.player.Player")
public class AirPlace implements ModuleAccess {
public static net.minecraft.world.phys.Vec3 highlightPos = null;
    public static float renderAlpha = 0.0f;
    public static double renderSize = 0.0;
    public static float renderR = 0.3f;
    public static float renderG = 0.7f;
    public static float renderB = 1.0f;
    @Parameter(name = "Mode", modes = {"Vanilla", "NCP"})
    public String mode = "Vanilla";
    @Parameter(name = "Render")
    public boolean render = true;
    @Parameter(name = "Animate")
    public boolean animate = true;
    @Parameter(name = "HighlightColor", color = true)
    public int highlightColor = 0xFF55AAFF;
    private final EasingAnimationUtility fadeAnim = new EasingAnimationUtility();
    private final EasingAnimationUtility sizeAnim = new EasingAnimationUtility();
    private final SlideAnimationUtility slideAnim = new SlideAnimationUtility();
    public net.minecraft.core.BlockPos currentTarget = null;
    private long lastPlaceTime = 0;
    public void onEnable() {
        highlightPos = null;
        renderAlpha = 0.0f;
        renderSize = 0.0;
        currentTarget = null;
        fadeAnim.reset();
        sizeAnim.reset();
        slideAnim.reset();
    }
    public void onDisable() {
        highlightPos = null;
        renderAlpha = 0.0f;
        renderSize = 0.0;
        currentTarget = null;
    }
    public void onTick() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null || mc.level == null) {
            highlightPos = null;
            return;
        }
        boolean mainHolding = InventoryUtility.isHoldingBlock(mc.player);
        var off = mc.player.getOffhandItem();
        boolean offHolding = !off.isEmpty() && off.getItem() instanceof BlockItem;
        var hand = mainHolding ? net.minecraft.world.InteractionHand.MAIN_HAND : (offHolding ? net.minecraft.world.InteractionHand.OFF_HAND : null);
        if (hand == null) {
            currentTarget = null;
            renderAlpha = fadeAnim.updateFloat(false, 0.25f);
            renderSize = sizeAnim.update(false, 0.15);
            if (renderAlpha <= 0.01f) highlightPos = null;
            return;
        }
        double dist = 4.5;
        var hit = mc.player.pick(dist, 1.0F, false);
        net.minecraft.core.BlockPos targetPos;
        net.minecraft.core.BlockPos neighbor;
        net.minecraft.core.Direction placeFace;
        if (hit != null && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            var bhr = (net.minecraft.world.phys.BlockHitResult) hit;
            neighbor = bhr.getBlockPos();
            placeFace = bhr.getDirection();
            targetPos = neighbor.relative(placeFace);
        } else {
            net.minecraft.world.phys.Vec3 eye = mc.player.getEyePosition(1.0F);
            net.minecraft.world.phys.Vec3 look = mc.player.getViewVector(1.0F);
            net.minecraft.world.phys.Vec3 target = eye.add(look.x * dist, look.y * dist, look.z * dist);
            targetPos = BlockUtility.containing(target.x, target.y, target.z);
            neighbor = targetPos;
            placeFace = net.minecraft.core.Direction.UP;
            for (net.minecraft.core.Direction face : net.minecraft.core.Direction.values()) {
                net.minecraft.core.BlockPos side = targetPos.relative(face);
                if (BlockUtility.isAir(mc.level, side)) continue;
                neighbor = side;
                placeFace = face.getOpposite();
                break;
            }
        }
        currentTarget = targetPos;
        if (render) {
            int hc = highlightColor;
            renderR = ((hc >> 16) & 0xFF) / 255.0f;
            renderG = ((hc >> 8) & 0xFF) / 255.0f;
            renderB = (hc & 0xFF) / 255.0f;
        }
        if (mc.options.keyUse.isDown()) {
            long now = System.currentTimeMillis();
            if (now - lastPlaceTime > 200) {
                if (mode.equals("NCP")) {
                    neighbor = targetPos;
                    placeFace = net.minecraft.core.Direction.UP;
                    for (net.minecraft.core.Direction face : net.minecraft.core.Direction.values()) {
                        net.minecraft.core.BlockPos side = targetPos.relative(face);
                        if (BlockUtility.isAir(mc.level, side)) continue;
                        neighbor = side;
                        placeFace = face.getOpposite();
                        break;
                    }
                    if (!BlockUtility.isAir(mc.level, neighbor)) {
                        double maxReach = 4.5;
                        net.minecraft.world.phys.Vec3 center = net.minecraft.world.phys.Vec3.atCenterOf(neighbor);
                        if (mc.player.getEyePosition().distanceTo(center) <= maxReach) {
                            net.minecraft.world.phys.Vec3 hitVec = center.add(
                                new net.minecraft.world.phys.Vec3(placeFace.getStepX(), placeFace.getStepY(), placeFace.getStepZ()).scale(0.5)
                            );
                            float[] angles = RotationUtility.anglesTo(mc.player.getEyePosition(), hitVec);
                            var conn = mc.getConnection();
                            if (conn != null) {
                                conn.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot(
                                    mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                                    angles[0], angles[1], mc.player.onGround(), mc.player.horizontalCollision
                                ));
                            }
                            BlockUtility.ncpAirPlace(ravex.mcwrapper.MinecraftWrapper.getWrapper(), neighbor, placeFace, hand);
                            if (conn != null) {
                                conn.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot(
                                    mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                                    mc.player.getYRot(), mc.player.getXRot(), mc.player.onGround(), mc.player.horizontalCollision
                                ));
                            }
                            SwingUtility.swing(mc.player, hand);
                        }
                    }
                } else {
                    net.minecraft.world.phys.Vec3 hitVec = net.minecraft.world.phys.Vec3.atCenterOf(neighbor).add(
                        new net.minecraft.world.phys.Vec3(placeFace.getStepX(), placeFace.getStepY(), placeFace.getStepZ()).scale(0.5)
                    );
                    net.minecraft.world.phys.BlockHitResult blockHit = new net.minecraft.world.phys.BlockHitResult(hitVec, placeFace, neighbor, false);
                    BlockUtility.useItemOn(ravex.mcwrapper.MinecraftWrapper.getWrapper(), blockHit, hand);
                    SwingUtility.swing(mc.player, hand);
                }
                lastPlaceTime = now;
            }
        }
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("AirPlace").getEnabled();
    }
    public static AirPlace itz() {
        return ravex.manager.ModuleManager.delegate(AirPlace.class);
    }


}