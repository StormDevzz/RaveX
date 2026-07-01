package ravex.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.render.Shaders;
import ravex.shaders.hand.HandShaderManager;
import ravex.utility.render.animate.ShaderVertexConsumer;

@Mixin(SubmitNodeStorage.class)
public class MixinSubmitNodeStorage {

    @Inject(
        method = "submitModelPart(Lnet/minecraft/client/model/geom/ModelPart;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ZZILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;I)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onSubmitModelPart(ModelPart modelPart, PoseStack poseStack, RenderType renderType, int light, int overlay, TextureAtlasSprite sprite, boolean glint, boolean glint2, int breakingId, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, int color, CallbackInfo ci) {
        if (!Shaders.RENDERING_HAND.get() || !Shaders.INSTANCE.getEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.renderBuffers() == null) return;

        HandShaderManager.init();
        if (!HandShaderManager.isInitialized()) return;

        VertexConsumer original = mc.renderBuffers().bufferSource().getBuffer(renderType);

        HandShaderManager.RenderInput renderInput = new HandShaderManager.RenderInput();
        renderInput.config = Shaders.INSTANCE.createConfig();
        renderInput.time = mc.level.getGameTime() + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        renderInput.deltaTime = mc.getDeltaTracker().getGameTimeDeltaTicks();

        VertexConsumer wrapped = new ShaderVertexConsumer(original, renderInput);

        modelPart.render(poseStack, wrapped, light, overlay, color);

        ci.cancel();
    }
}
