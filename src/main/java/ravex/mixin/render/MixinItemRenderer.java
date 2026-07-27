package ravex.mixin.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.render.Glint;
import ravex.utility.render.GlintVertexConsumerUtility;
import ravex.modules.Modules;

@Mixin(ItemRenderer.class)
public class MixinItemRenderer {

    @Inject(method = "getFoilBuffer(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/rendertype/RenderType;ZZ)Lcom/mojang/blaze3d/vertex/VertexConsumer;", at = @At("RETURN"), cancellable = true)
    private static void onGetFoilBuffer(MultiBufferSource bufferSource, RenderType renderType, boolean isItem, boolean glint, CallbackInfoReturnable<VertexConsumer> cir) {
        if (Modules.enabled(Glint.class) && glint) {
            if (isItem && Modules.get(Glint.class).items) {
                cir.setReturnValue(new GlintVertexConsumerUtility(cir.getReturnValue(), Modules.get(Glint.class).color));
            } else if (!isItem && Modules.get(Glint.class).armor) {
                cir.setReturnValue(new GlintVertexConsumerUtility(cir.getReturnValue(), Modules.get(Glint.class).color));
            }
        }
    }
}
