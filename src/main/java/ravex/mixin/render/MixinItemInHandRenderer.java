package ravex.mixin.render;

import net.minecraft.client.renderer.ItemInHandRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.render.Shaders;

@Mixin(ItemInHandRenderer.class)
public abstract class MixinItemInHandRenderer {

    @Inject(
        method = "renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V",
        at = @At("HEAD")
    )
    private void onRenderHead(float f, PoseStack poseStack, SubmitNodeCollector collector, LocalPlayer player, int i, CallbackInfo ci) {
        Shaders.RENDERING_HAND.set(true);
    }

    @Inject(
        method = "renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V",
        at = @At("RETURN")
    )
    private void onRenderReturn(float f, PoseStack poseStack, SubmitNodeCollector collector, LocalPlayer player, int i, CallbackInfo ci) {
        Shaders.RENDERING_HAND.set(false);
    }
}
