package ravex.mixin.render;

import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.render.NoRender;

@Mixin(HumanoidArmorLayer.class)
public class MixinHumanoidArmorLayer {
    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V", at = @At("HEAD"), cancellable = true)
    private void onSubmit(PoseStack poseStack, SubmitNodeCollector collector, int light, HumanoidRenderState state, float f1, float f2, CallbackInfo ci) {
        if (NoRender.INSTANCE.getEnabled() && NoRender.INSTANCE.armor.getValue()) {
            ci.cancel();
        }
    }
}
