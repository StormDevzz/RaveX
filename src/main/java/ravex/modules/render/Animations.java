package ravex.modules.render;
import ravex.manager.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import java.util.List;
public class Animations extends Module {
    public final ModeParameter walkingAnimation = new ModeParameter("WalkAnimation", "Smooth",
            List.of("Smooth", "Bouncy", "Robotic", "Off"));
    public final ModeParameter sprintAnimation = new ModeParameter("SprintAnimation", "Smooth",
            List.of("Smooth", "Bouncy", "Robotic", "Off"));
    public final ModeParameter sneakAnimation = new ModeParameter("SneakAnimation", "Smooth",
            List.of("Smooth", "Crawl", "Off"));
    public final ModeParameter jumpAnimation = new ModeParameter("JumpAnimation", "Normal",
            List.of("Normal", "Tuck", "Spread", "Off"));
    public final ModeParameter swimmingAnimation = new ModeParameter("SwimAnimation", "Smooth",
            List.of("Smooth", "Dolphin", "Off"));
    public final ModeParameter elytraAnimation = new ModeParameter("ElytraAnimation", "Smooth",
            List.of("Smooth", "Bird", "Off"));
    public final NumberParameter animationSpeed = new NumberParameter("Speed", 1.0, 0.5, 2.0, 0.1);
    public final BooleanParameter smoothTransitions = new BooleanParameter("SmoothTransitions", true);
    public final BooleanParameter disableVanillaSwing = new BooleanParameter("DisableVanillaSwing", false);
    public static float swingProgress = 0f;
    public static float prevSwingProgress = 0f;
    public static float bodyBob = 0f;
    public static float limbSwing = 0f;
    public static float headTilt = 0f;
    private float walkCycle = 0f;

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        float speed = animationSpeed.getValue().floatValue();
        boolean onGround = player.onGround();
        boolean isSprinting = player.isSprinting();
        boolean isSneaking = player.isShiftKeyDown();
        boolean isSwimming = player.isSwimming();
        boolean isFlying = player.isFallFlying();
        boolean isJumping = !onGround && player.getDeltaMovement().y > 0;
        boolean isFalling = !onGround && player.getDeltaMovement().y < 0;
        float moveX = (float) (player.getX() - player.xo);
        float moveZ = (float) (player.getZ() - player.zo);
        float moveSpeed = (float) Math.sqrt(moveX * moveX + moveZ * moveZ);
        if (moveSpeed > 0.01f && onGround) {
            walkCycle += moveSpeed * 20f * speed;
            if (walkCycle > Float.MAX_VALUE) walkCycle = 0;
        } else if (!onGround) {
            walkCycle += 0.02f * speed;
        }
        limbSwing = (float) Math.sin(walkCycle);
        bodyBob = (float) Math.abs(Math.sin(walkCycle * 2f));
        if (isSprinting) {
            limbSwing *= 1.5f;
            bodyBob *= 1.3f;
        }
        if (isSneaking) {
            limbSwing *= 0.5f;
            bodyBob *= 0.3f;
        }
        if (isJumping) {
            String jumpMode = jumpAnimation.getValue();
            if (jumpMode.equals("Tuck")) {
                limbSwing = 0.3f;
                bodyBob = 0.5f;
            } else if (jumpMode.equals("Spread")) {
                limbSwing = -0.5f;
                bodyBob = 0.2f;
            }
        }
        if (isFalling && !isFlying) {
            limbSwing *= 1.2f;
        }
        if (isFlying) {
            String elytraMode = elytraAnimation.getValue();
            if (elytraMode.equals("Bird")) {
                limbSwing = (float) Math.sin(walkCycle * 3f) * 0.6f;
                bodyBob = (float) Math.abs(Math.sin(walkCycle * 2f)) * 0.4f;
            } else {
                limbSwing = 0.2f;
                bodyBob = 0.1f;
            }
        }
        if (isSwimming) {
            String swimMode = swimmingAnimation.getValue();
            if (swimMode.equals("Dolphin")) {
                limbSwing = (float) Math.sin(walkCycle * 2.5f) * 0.8f;
                bodyBob = (float) Math.abs(Math.sin(walkCycle * 2f)) * 0.5f;
            } else {
                limbSwing *= 0.7f;
                bodyBob *= 0.5f;
            }
        }
        if (isSneaking) {
            String sneakMode = sneakAnimation.getValue();
            if (sneakMode.equals("Crawl")) {
                player.setPose(Pose.SWIMMING);
            }
        }
        if (smoothTransitions.getValue()) {
            prevSwingProgress = swingProgress;
            swingProgress = limbSwing;
        } else {
            prevSwingProgress = 0;
            swingProgress = limbSwing;
        }
        if (disableVanillaSwing.getValue()) {
            player.swinging = false;
        }
        applyHeadAnimations(player);
    }
    private void applyHeadAnimations(LocalPlayer player) {
        float speed = animationSpeed.getValue().floatValue();
        boolean isSprinting = player.isSprinting();
        boolean isSneaking = player.isShiftKeyDown();
        headTilt = 0f;
        if (isSprinting && !sprintAnimation.getValue().equals("Off")) {
            headTilt = (float) Math.sin(walkCycle * 1.5f) * 0.03f;
        }
        if (isSneaking) {
            headTilt = -0.08f;
            if (sneakAnimation.getValue().equals("Crawl")) {
                headTilt = -0.25f;
            }
        }
        if (player.isSwimming()) {
            headTilt = -0.1f;
        }
        if (player.isFallFlying()) {
            headTilt = -0.05f;
        }
    }
    public static float getLimbSwing(float partialTicks) {
        if (!ModuleManager.get(Animations.class).getEnabled()) return 0f;
        if (ModuleManager.get(Animations.class).smoothTransitions.getValue()) {
            return ModuleManager.get(Animations.class).prevSwingProgress + (ModuleManager.get(Animations.class).swingProgress - ModuleManager.get(Animations.class).prevSwingProgress) * partialTicks;
        }
        return ModuleManager.get(Animations.class).swingProgress;
    }
    public static float getBodyBob(float partialTicks) {
        if (!ModuleManager.get(Animations.class).getEnabled()) return 0f;
        return ModuleManager.get(Animations.class).bodyBob;
    }
    public static float getHeadTilt() {
        if (!ModuleManager.get(Animations.class).getEnabled()) return 0f;
        return ModuleManager.get(Animations.class).headTilt;
    }
    public static float getSwingProgress(float partialTicks) {
        if (!ModuleManager.get(Animations.class).getEnabled()) return 0f;
        if (ModuleManager.get(Animations.class).disableVanillaSwing.getValue()) return 0f;
        if (ModuleManager.get(Animations.class).smoothTransitions.getValue()) {
            return ModuleManager.get(Animations.class).prevSwingProgress + (ModuleManager.get(Animations.class).swingProgress - ModuleManager.get(Animations.class).prevSwingProgress) * partialTicks;
        }
        return ModuleManager.get(Animations.class).swingProgress;
    }
    @Override
    protected void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getPose() == Pose.SWIMMING) {
            if (!mc.player.isSwimming()) {
                mc.player.setPose(Pose.STANDING);
            }
        }
        swingProgress = 0f;
        prevSwingProgress = 0f;
        bodyBob = 0f;
        limbSwing = 0f;
        headTilt = 0f;
        walkCycle = 0f;
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(Animations.class);
    }

    public static Animations itz() {
        return ModuleManager.get(Animations.class);
    }
}
