package ravex.mixin.render;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.opengl.GlStateManager;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.manager.HandShaderManager;
import ravex.manager.PlayerShaderManager;
import ravex.modules.render.Shaders;
import ravex.modules.Modules;
import ravex.utility.shaders.EffectInput;
import ravex.utility.shaders.EffectOutput;
import ravex.utility.shaders.EffectType;
import ravex.utility.shaders.ShaderConfig;
import ravex.utility.shaders.ShaderPipeline;

@Mixin(ItemStackRenderState.class)
public class MixinItemStackRenderState {

    private static PoseStack ravex$itemPose;
    private static final EffectInput ravex$effectInput = new EffectInput();

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
        if (shaders == null || !Modules.enabled(Shaders.class) || !shaders.items) return tint;
        boolean renderPlayer = Shaders.RENDERING_PLAYER.get();
        boolean renderHand = Shaders.RENDERING_HAND.get();
        if (!renderPlayer && !renderHand) return tint;

        boolean isHand = renderHand;
        ShaderPipeline pipeline;
        if (isHand) {
            if (!HandShaderManager.isInitialized()) HandShaderManager.init();
            pipeline = HandShaderManager.getPipeline();
        } else {
            if (!PlayerShaderManager.isInitialized()) PlayerShaderManager.init();
            pipeline = PlayerShaderManager.getPipeline();
        }

        ShaderConfig cfg = shaders.createConfig();
        if (cfg.effect == EffectType.NONE) return shaders.fillColor;
        pipeline.setConfig(cfg);

        float x = 0f;
        float y = 0f;
        float z = 0f;
        if (ravex$itemPose != null) {
            Matrix4f pose = ravex$itemPose.last().pose();
            x = pose.m30();
            y = pose.m31();
            z = pose.m32();
        }

        ravex$effectInput.vertex.position.x = x;
        ravex$effectInput.vertex.position.y = y;
        ravex$effectInput.vertex.position.z = z;
        ravex$effectInput.worldPos.x = x;
        ravex$effectInput.worldPos.y = y;
        ravex$effectInput.worldPos.z = z;
        ravex$effectInput.localPos.x = x;
        ravex$effectInput.localPos.y = y;
        ravex$effectInput.localPos.z = z;
        ravex$effectInput.normalizedTime = Shaders.tickTime();
        ravex$effectInput.deltaTime = 0f;
        ravex$effectInput.intensity = 1f;

        EffectOutput out = pipeline.processVertex(ravex$effectInput);
        float blend = out.alpha;
        if (blend < 0f) blend = 0f;
        if (blend > 1f) blend = 1f;

        int fillColor = shaders.fillColor;
        float br = ((fillColor >> 16) & 0xFF) / 255f;
        float bg = ((fillColor >> 8) & 0xFF) / 255f;
        float bb = (fillColor & 0xFF) / 255f;
        float ba = ((fillColor >> 24) & 0xFF) / 255f;

        float r = br * (1f - blend) + out.color.r * blend;
        float g = bg * (1f - blend) + out.color.g * blend;
        float b = bb * (1f - blend) + out.color.b * blend;

        int ir = (int) (r * 255f);
        int ig = (int) (g * 255f);
        int ib = (int) (b * 255f);
        int ia = (int) (ba * 255f);
        if (ir > 255) ir = 255;
        if (ig > 255) ig = 255;
        if (ib > 255) ib = 255;
        if (ia > 255) ia = 255;
        return (ia << 24) | (ir << 16) | (ig << 8) | ib;
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

    @Inject(
        method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V",
        at = @At("HEAD")
    )
    private void onSubmitHead(PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, int tint, CallbackInfo ci) {
        Shaders shaders = Modules.get(Shaders.class);
        if (shaders == null || !Modules.enabled(Shaders.class) || !shaders.items || !shaders.throughWalls) return;
        boolean renderPlayer = Shaders.RENDERING_PLAYER.get();
        boolean renderHand = Shaders.RENDERING_HAND.get();
        if (renderPlayer || renderHand) {
            GlStateManager._disableDepthTest();
        }
    }

    @Inject(
        method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V",
        at = @At("RETURN")
    )
    private void onSubmitReturn(PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, int tint, CallbackInfo ci) {
        Shaders shaders = Modules.get(Shaders.class);
        if (shaders == null || !Modules.enabled(Shaders.class) || !shaders.items || !shaders.throughWalls) return;
        boolean renderPlayer = Shaders.RENDERING_PLAYER.get();
        boolean renderHand = Shaders.RENDERING_HAND.get();
        if (renderPlayer || renderHand) {
            GlStateManager._enableDepthTest();
        }
    }
}
