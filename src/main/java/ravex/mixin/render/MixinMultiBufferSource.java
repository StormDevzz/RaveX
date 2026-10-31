package ravex.mixin.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.render.Glint;
import ravex.utility.render.GlintTextureUtility;
import ravex.modules.Modules;

// keeps the glint textures in sync with the glint module
@Mixin(MultiBufferSource.BufferSource.class)
public class MixinMultiBufferSource {

    @Inject(method = "getBuffer", at = @At("HEAD"))
    private void onGetBuffer(RenderType type, CallbackInfoReturnable<VertexConsumer> cir) {
        if (type == null) return;
        if (type != RenderTypes.glint() && type != RenderTypes.glintTranslucent()
                && type != RenderTypes.entityGlint() && type != RenderTypes.armorEntityGlint()) {
            return;
        }
        if (Modules.enabled(Glint.class)) {
            Glint glint = Modules.get(Glint.class);
            GlintTextureUtility.ensure(glint.color, glint.items, glint.armor);
        } else {
            GlintTextureUtility.ensureOriginal();
        }
    }
}
