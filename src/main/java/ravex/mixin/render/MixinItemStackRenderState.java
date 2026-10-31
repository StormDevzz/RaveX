package ravex.mixin.render;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ravex.modules.render.Shaders;
import ravex.modules.Modules;
import ravex.utility.shaders.ShaderFx;

@Mixin(ItemStackRenderState.class)
public class MixinItemStackRenderState {

    private static PoseStack ravex$itemPose;

    @ModifyVariable(
        method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V",
        at = @At("HEAD"),
        ordinal = 0,
        argsOnly = true
    )
    private PoseStack ravex$capturePose(PoseStack poseStack) {
        ravex$itemPose = poseStack;
        return poseStack;
    }

    private static int ravex$computeTint(int tint) {
        Shaders shaders = Modules.get(Shaders.class);
        if (shaders == null || !Modules.enabled(Shaders.class)) return tint;
        boolean renderPlayer = Shaders.RENDERING_PLAYER.get();
        boolean renderHand = Shaders.RENDERING_HAND.get();
        if (!renderPlayer && !renderHand) return tint;
        // hand has its own toggle
        if (renderHand && !shaders.hand) return tint;
        // world and held items use the items toggle
        if (renderPlayer && !shaders.items) return tint;

        int fx = shaders.fx();
        if (fx == ShaderFx.STATIC || !ShaderFx.available()) return shaders.appliedColor();

        float x = 0f;
        float y = 0f;
        float z = 0f;
        if (ravex$itemPose != null) {
            Matrix4f pose = ravex$itemPose.last().pose();
            x = pose.m30();
            y = pose.m31();
            z = pose.m32();
        }
        return ShaderFx.tint(fx, Shaders.tickTime(), shaders.appliedColor(), shaders.appliedAlpha(), x, y, z);
    }

    @ModifyVariable(
        method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V",
        at = @At("HEAD"),
        ordinal = 2,
        argsOnly = true
    )
    private int modifyTint(int tint) {
        return ravex$computeTint(tint);
    }
}
