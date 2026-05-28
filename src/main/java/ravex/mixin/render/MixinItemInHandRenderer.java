package ravex.mixin.render;

import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.player.LocalPlayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.render.Shaders;
import ravex.manager.ShaderManager;

@Mixin(ItemInHandRenderer.class)
public abstract class MixinItemInHandRenderer {

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"))
    private void onRenderHandsWithItemsHead(float tickDelta, PoseStack poseStack, SubmitNodeCollector collector, LocalPlayer player, int combinedLight, CallbackInfo ci) {
        if (Shaders.INSTANCE.getEnabled() && Shaders.INSTANCE.hands.getValue()) {
            ShaderManager.INSTANCE.renderingHand = true;
        }
    }

    @Inject(method = "renderHandsWithItems", at = @At("RETURN"))
    private void onRenderHandsWithItemsReturn(float tickDelta, PoseStack poseStack, SubmitNodeCollector collector, LocalPlayer player, int combinedLight, CallbackInfo ci) {
        ShaderManager.INSTANCE.renderingHand = false;
    }
}
