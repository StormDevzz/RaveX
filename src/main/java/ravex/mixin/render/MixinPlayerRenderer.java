package ravex.mixin.render;

import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public class MixinPlayerRenderer {

    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
        at = @At("TAIL")
    )
    private void onExtractPlayerRenderState(net.minecraft.world.entity.Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
        if (entity instanceof net.minecraft.world.entity.player.Player player) {
            
            if (ravex.modules.render.ShiftInterp.INSTANCE.getEnabled()) {
                if (ravex.modules.render.ShiftInterp.INSTANCE.shouldCrouch(player)) {
                    state.isCrouching = true;
                }
            }

            
            if (ravex.modules.render.BabyDude.INSTANCE.getEnabled()) {
                if (ravex.modules.render.BabyDude.INSTANCE.shouldScale(player)) {
                    state.isBaby = true;
                    ravex.modules.render.BabyDude.INSTANCE.stateScaleMap.put(state, ravex.modules.render.BabyDude.INSTANCE.scale.getValue().floatValue());
                } else {
                    ravex.modules.render.BabyDude.INSTANCE.stateScaleMap.remove(state);
                }
            } else {
                ravex.modules.render.BabyDude.INSTANCE.stateScaleMap.remove(state);
            }
        }
    }

    @Inject(
        method = "scale(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V",
        at = @At("TAIL")
    )
    private void onScalePlayer(AvatarRenderState state, PoseStack poseStack, CallbackInfo ci) {
        Float s = ravex.modules.render.BabyDude.INSTANCE.stateScaleMap.get(state);
        if (s != null) {
            poseStack.scale(s, s, s);
        }
    }
}
