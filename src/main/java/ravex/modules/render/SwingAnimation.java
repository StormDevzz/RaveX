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
            List.of("Default", "Swipe", "Akrien", "Rich"));
    public final NumberParameter speed = new NumberParameter("Speed", 1.0, 0.1, 5.0, 0.1);

    private SwingAnimation() {
        super("SwingAnimation");
    }

    public void applyFourteen(PoseStack poseStack, float swingProgress, float equipProgress) {
        // как в thunderhack fourteen: тупо по swingProgress
        if (swingProgress > 0) {
            float g = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
            float y = Mth.lerp(g, -0.52F + equipProgress * -0.6F, equipProgress * -0.2f - 0.5F);
            float z = Mth.lerp(g, -0.72F, -0.7F);
            poseStack.translate(0.56F, y, z);
            poseStack.mulPose(Axis.YP.rotationDegrees(g * 45.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(g * -85.0F));
            poseStack.translate(g * -0.1F, g * 0.28F, g * 0.2F);
            poseStack.mulPose(Axis.XP.rotationDegrees(g * -85.0F));
        } else {
            // idle — обычная позиция
            poseStack.translate(0.56F, -0.52F + equipProgress * -0.6F, -0.72F);
        }
    }

    public void applySwipe(PoseStack poseStack, float swingProgress, float equipProgress) {
        // как в thunderhack nine: equipProgress = 0
        float g = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        poseStack.translate(0.56F, -0.52F, -0.72F);
        poseStack.mulPose(Axis.XP.rotationDegrees(50f));
        poseStack.mulPose(Axis.YP.rotationDegrees(-30f * (1f - g) - 30f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(110f));
    }

    public void applyRich(PoseStack poseStack, float swingProgress, float equipProgress, boolean rightHand) {
        float g = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        int i = rightHand ? 1 : -1;
        poseStack.translate(i * 0.56F, -0.52F, -0.72F);
        poseStack.mulPose(Axis.YP.rotationDegrees(30f));
        poseStack.mulPose(Axis.XP.rotationDegrees(-70f));
        poseStack.mulPose(Axis.YP.rotationDegrees(30f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(g * -20f));
        poseStack.mulPose(Axis.XP.rotationDegrees(g * -75f));
    }

    public void applyDefault(PoseStack poseStack, float swingProgress, float equipProgress, boolean rightHand) {
        int i = rightHand ? 1 : -1;
        poseStack.translate(i * 0.56F, -0.52F + equipProgress * -0.6F, -0.72F);

        ViewModel vm = ViewModel.itz();
        boolean vmEnabled = vm.getEnabled();
        if (vmEnabled) {
            float mx = vm.mainX.getValue().floatValue();
            float my = vm.mainY.getValue().floatValue();
            float mz = vm.mainZ.getValue().floatValue();
            poseStack.translate(-mx, my, mz);
        }

        float f = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
        float g = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        poseStack.mulPose(Axis.YP.rotationDegrees(i * (45.0F + f * -20.0F)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(i * g * -20.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(g * -80.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(i * -45.0F));

        if (vmEnabled) {
            float mx = vm.mainX.getValue().floatValue();
            float my = vm.mainY.getValue().floatValue();
            float mz = vm.mainZ.getValue().floatValue();
            poseStack.translate(mx, -my, -mz);
        }
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(SwingAnimation.class);
    }

    public static SwingAnimation itz() {
        return ModuleManager.get(SwingAnimation.class);
    }
}
