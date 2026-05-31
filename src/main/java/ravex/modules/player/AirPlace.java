package ravex.modules.player;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.utility.render.animate.FadeAnimation;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class AirPlace extends Module {
    public static final AirPlace INSTANCE = new AirPlace();

    // Static state for MixinLevelRenderer to consume
    public static Vec3 highlightPos = null;
    public static float renderAlpha = 0.0f;
    public static float renderR = 0.2f;
    public static float renderG = 0.8f;
    public static float renderB = 1.0f;

    public final BooleanParameter render = new BooleanParameter("Render", true);

    private final FadeAnimation fadeAnim = new FadeAnimation();
    private BlockPos currentTarget = null;
    private long lastPlaceTime = 0;

    private AirPlace() {
        super("AirPlace", Category.PLAYER);
        addParameter(render);
    }

    @Override
    protected void onEnable() {
        highlightPos = null;
        renderAlpha = 0.0f;
        currentTarget = null;
        fadeAnim.reset();
    }

    @Override
    protected void onDisable() {
        highlightPos = null;
        renderAlpha = 0.0f;
        currentTarget = null;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            highlightPos = null;
            return;
        }

        ItemStack main = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack off = mc.player.getItemInHand(InteractionHand.OFF_HAND);
        boolean mainHolding = !main.isEmpty() && main.getItem() instanceof net.minecraft.world.item.BlockItem;
        boolean offHolding = !off.isEmpty() && off.getItem() instanceof net.minecraft.world.item.BlockItem;
        InteractionHand hand = mainHolding ? InteractionHand.MAIN_HAND : (offHolding ? InteractionHand.OFF_HAND : null);

        if (hand == null) {
            currentTarget = null;
            renderAlpha = fadeAnim.update(false, 0.25f);
            if (renderAlpha <= 0.01f) {
                highlightPos = null;
            }
            return;
        }

        double dist = 4.5;
        net.minecraft.world.phys.HitResult hit = mc.player.pick(dist, 1.0F, false);

        BlockPos targetPos;
        BlockPos neighbor;
        net.minecraft.core.Direction placeFace;

        if (hit != null && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            net.minecraft.world.phys.BlockHitResult bhr = (net.minecraft.world.phys.BlockHitResult) hit;
            neighbor = bhr.getBlockPos();
            placeFace = bhr.getDirection();
            targetPos = neighbor.relative(placeFace);
        } else {
            Vec3 eye = mc.player.getEyePosition(1.0F);
            Vec3 look = mc.player.getViewVector(1.0F);
            Vec3 target = eye.add(look.x * dist, look.y * dist, look.z * dist);
            targetPos = BlockPos.containing(target);
            neighbor = targetPos;
            placeFace = net.minecraft.core.Direction.UP;
            for (net.minecraft.core.Direction face : net.minecraft.core.Direction.values()) {
                BlockPos side = targetPos.relative(face);
                if (!mc.level.getBlockState(side).isAir()) {
                    neighbor = side;
                    placeFace = face.getOpposite();
                    break;
                }
            }
        }

        currentTarget = targetPos;

        if (render.getValue()) {
            renderAlpha = fadeAnim.update(true, 0.25f);
            highlightPos = Vec3.atLowerCornerOf(targetPos);
        } else {
            highlightPos = null;
            renderAlpha = 0.0f;
        }

        if (mc.options.keyUse.isDown()) {
            long now = System.currentTimeMillis();
            if (now - lastPlaceTime > 200) {
                Vec3 hitVec = Vec3.atCenterOf(neighbor).add(
                    new Vec3(placeFace.getStepX(), placeFace.getStepY(), placeFace.getStepZ()).scale(0.5)
                );
                net.minecraft.world.phys.BlockHitResult blockHit = new net.minecraft.world.phys.BlockHitResult(
                    hitVec, placeFace, neighbor, false
                );
                mc.gameMode.useItemOn(mc.player, hand, blockHit);
                mc.player.swing(hand);
                lastPlaceTime = now;
            }
        }
    }
}
