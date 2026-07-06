package ravex.mixin.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.render.Glint;
import ravex.utility.render.GlintVertexConsumer;

@Mixin(MultiBufferSource.BufferSource.class)
public class MixinMultiBufferSource {

    @Inject(method = "getBuffer", at = @At("RETURN"), cancellable = true)
    private void onGetBuffer(RenderType type, CallbackInfoReturnable<VertexConsumer> cir) {
        if (type == null) return;
        String name = type.toString().toLowerCase();
        if (name.contains("glint")) {
            boolean isArmor = name.contains("armor");
            if (isArmor) {
                if (Glint.INSTANCE.getEnabled() && Glint.INSTANCE.armor.getValue()) {
                    cir.setReturnValue(new GlintVertexConsumer(cir.getReturnValue(), Glint.INSTANCE.color.getValue()));
                }
            } else {
                if (Glint.INSTANCE.getEnabled() && Glint.INSTANCE.items.getValue()) {
                    cir.setReturnValue(new GlintVertexConsumer(cir.getReturnValue(), Glint.INSTANCE.color.getValue()));
                }
            }
        }
    }
}
