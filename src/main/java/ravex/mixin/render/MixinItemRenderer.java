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
import ravex.utility.render.GlintVertexConsumer;

@Mixin(ItemRenderer.class)
public class MixinItemRenderer {

    @Inject(method = "getFoilBuffer(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/rendertype/RenderType;ZZ)Lcom/mojang/blaze3d/vertex/VertexConsumer;", at = @At("RETURN"), cancellable = true)
    private static void onGetFoilBuffer(MultiBufferSource bufferSource, RenderType renderType, boolean isItem, boolean glint, CallbackInfoReturnable<VertexConsumer> cir) {
        if (Glint.INSTANCE.getEnabled() && glint) {
            if (isItem && Glint.INSTANCE.items.getValue()) {
                cir.setReturnValue(new GlintVertexConsumer(cir.getReturnValue(), Glint.INSTANCE.color.getValue()));
            } else if (!isItem && Glint.INSTANCE.armor.getValue()) {
                cir.setReturnValue(new GlintVertexConsumer(cir.getReturnValue(), Glint.INSTANCE.color.getValue()));
            }
        }
    }
}
