package ravex.modules.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;

import java.util.List;

public class SwingAnimation extends Module {
    public final ModeParameter mode = new ModeParameter("Mode", "Akrien",
            List.of("Default", "Swipe", "Akrien"));
    public final NumberParameter speed = new NumberParameter("Speed", 1.0, 0.1, 5.0, 0.1);

    public float akrienBlend = 0.0f;
    private long lastTime = System.nanoTime();

    private SwingAnimation() {
        super("SwingAnimation");
    }

    public void applyFourteen(PoseStack poseStack, float swingProgress, float equipProgress) {
        long now = System.nanoTime();
        float delta = (now - lastTime) / 1e9f;
        lastTime = now;
        if (delta > 0.1f) delta = 0.1f;

        if (swingProgress > 0.0f) {
            akrienBlend = 1.0f;
            lastTime = now;
        } else {
            akrienBlend -= delta * 5.5f * speed.getValue().floatValue();
            if (akrienBlend < 0.0f) akrienBlend = 0.0f;
        }

        if (akrienBlend <= 0.0f) {
            poseStack.translate(0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
            return;
        }

        float g = swingProgress > 0.0f ? Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI) : 0.0f;

        float tx1 = 0.56F;
        float ty1_akrien = equipProgress * -0.2F - 0.5F;
        float ty1_idle = -0.52F + equipProgress * -0.6F;
        float ty1 = ty1_idle + (ty1_akrien - ty1_idle) * akrienBlend;
        float tz1 = -0.72F + (-0.7F - (-0.72F)) * akrienBlend;

        poseStack.translate(tx1, ty1, tz1);

        float ry = 45.0F * akrienBlend;
        float rx = g * -85.0F * akrienBlend;
        poseStack.mulPose(Axis.YP.rotationDegrees(ry));
        poseStack.mulPose(Axis.XP.rotationDegrees(rx));

        float tx2 = -0.1F * akrienBlend;
        float ty2 = 0.28F * akrienBlend;
        float tz2 = 0.2F * akrienBlend;
        poseStack.translate(tx2, ty2, tz2);

        float rx2 = -85.0F * akrienBlend;
        poseStack.mulPose(Axis.XP.rotationDegrees(rx2));
    }

    public void applySwipe(PoseStack poseStack, float swingProgress, float equipProgress) {
        float g = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);

        poseStack.translate(0.56F, -0.52F + equipProgress * -0.6F, -0.72F);

        poseStack.mulPose(Axis.XP.rotationDegrees(50f));
        poseStack.mulPose(Axis.YP.rotationDegrees(-30f * (1f - g) - 30f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(110f));
    }

    public void applyDefault(PoseStack poseStack, float swingProgress, float equipProgress) {
        float f = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
        float g = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);

        poseStack.translate(0.56F, -0.52F + equipProgress * -0.6F, -0.72F);

        poseStack.mulPose(Axis.YP.rotationDegrees(45.0F + f * -20.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(g * -20.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(g * -80.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(-45.0F));

        if (swingProgress > 0.0f) {
            float scale = 1.0f + 0.1f * Mth.sin(swingProgress * (float) Math.PI);
            poseStack.scale(scale, scale, scale);
        }
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(SwingAnimation.class);
    }

    public static SwingAnimation itz() {
        return ModuleManager.get(SwingAnimation.class);
    }
}
