package ravex.modules.player;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.utility.render.animate.SlideAnimation;
import ravex.utility.render.animate.FadeAnimation;
import ravex.utility.render.animate.SizeAnimation;
import ravex.utility.render.animate.BounceAnimation;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class AirPlace extends Module {
    public static final AirPlace INSTANCE = new AirPlace();
    
    // Static animation states for MixinLevelRenderer to query
    public static Vec3 highlightPos = null;
    public static float renderAlpha = 0.0f;
    public static double renderSize = 0.0;
    public static double renderYOffset = 0.0;

    // Render configuration parameter
    public final BooleanParameter render = new BooleanParameter("Render", true);

    // Animation utilities instances
    private final SlideAnimation slideAnim = new SlideAnimation();
    private final FadeAnimation fadeAnim = new FadeAnimation();
    private final SizeAnimation sizeAnim = new SizeAnimation();
    private final BounceAnimation bounceAnim = new BounceAnimation();

    private long lastPlaceTime = 0;

    private AirPlace() {
        super("AirPlace", Category.PLAYER);
        addParameter(render);
    }

    @Override
    protected void onEnable() {
        highlightPos = null;
        renderAlpha = 0.0f;
        renderSize = 0.0;
        renderYOffset = 0.0;
        slideAnim.reset();
        fadeAnim.reset();
        sizeAnim.reset();
        bounceAnim.reset();
    }

    @Override
    protected void onDisable() {
        highlightPos = null;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            highlightPos = null;
            return;
        }

        // 1. Check if holding a block in either hand and select the appropriate hand
        ItemStack main = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack off = mc.player.getItemInHand(InteractionHand.OFF_HAND);
        boolean mainHolding = !main.isEmpty() && main.getItem() instanceof net.minecraft.world.item.BlockItem;
        boolean offHolding = !off.isEmpty() && off.getItem() instanceof net.minecraft.world.item.BlockItem;
        InteractionHand hand = mainHolding ? InteractionHand.MAIN_HAND : (offHolding ? InteractionHand.OFF_HAND : null);

        if (hand == null) {
            // Smoothly fade out when not holding blocks
            renderAlpha = fadeAnim.update(false, 0.15f);
            renderSize = sizeAnim.update(false, 0.15f);
            if (renderAlpha <= 0.01f) {
                highlightPos = null;
            }
            return;
        }

        // 2. Perform pick raycasting to find target block position in front of player
        double dist = 4.5;
        net.minecraft.world.phys.HitResult hit = mc.player.pick(dist, 1.0F, false);
        BlockPos targetPos;
        if (hit != null && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            targetPos = ((net.minecraft.world.phys.BlockHitResult) hit).getBlockPos();
        } else {
            Vec3 eye = mc.player.getEyePosition(1.0F);
            Vec3 look = mc.player.getViewVector(1.0F);
            Vec3 target = eye.add(look.x * dist, look.y * dist, look.z * dist);
            targetPos = BlockPos.containing(target);
        }

        // 3. Update animations smoothly
        if (render.getValue()) {
            renderAlpha = fadeAnim.update(true, 0.15f);
            renderSize = sizeAnim.update(true, 0.15f);
            renderYOffset = bounceAnim.update(0.05, 0.04);
            highlightPos = slideAnim.update(targetPos.getX(), targetPos.getY() + renderYOffset, targetPos.getZ(), 0.25);
        } else {
            // Instantly hide
            highlightPos = null;
            renderAlpha = 0.0f;
            renderSize = 0.0;
        }

        // 4. Place blocks if right-click key is pressed down, with a 200ms safe interval
        if (mc.options.keyUse.isDown()) {
            long now = System.currentTimeMillis();
            if (now - lastPlaceTime > 200) {
                Vec3 hitVec = Vec3.atCenterOf(targetPos);
                net.minecraft.world.phys.BlockHitResult blockHit = new net.minecraft.world.phys.BlockHitResult(
                    hitVec, net.minecraft.core.Direction.UP, targetPos, false
                );
                mc.gameMode.useItemOn(mc.player, hand, blockHit);
                mc.player.swing(hand);
                lastPlaceTime = now;
            }
        }
    }
}
