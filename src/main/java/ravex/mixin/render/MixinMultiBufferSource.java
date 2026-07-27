package ravex.mixin.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.render.Glint;
import ravex.utility.render.GlintVertexConsumerUtility;
import ravex.modules.Modules;

@Mixin(MultiBufferSource.BufferSource.class)
public class MixinMultiBufferSource {

    @Inject(method = "getBuffer", at = @At("RETURN"), cancellable = true)
    private void onGetBuffer(RenderType type, CallbackInfoReturnable<VertexConsumer> cir) {
        if (type == null) return;
        String name = type.toString().toLowerCase();
        if (name.contains("glint")) {
            boolean isArmor = name.contains("armor");
            if (isArmor) {
                if (Modules.enabled(Glint.class) && Modules.get(Glint.class).armor) {
                    cir.setReturnValue(new GlintVertexConsumerUtility(cir.getReturnValue(), Modules.get(Glint.class).color));
                }
            } else {
                if (Modules.enabled(Glint.class) && Modules.get(Glint.class).items) {
                    cir.setReturnValue(new GlintVertexConsumerUtility(cir.getReturnValue(), Modules.get(Glint.class).color));
                }
            }
        }
    }
}
