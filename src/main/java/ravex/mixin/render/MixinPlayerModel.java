package ravex.mixin.render;

import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.render.Animation;

@Mixin(value = PlayerModel.class, priority = 9999)
public class MixinPlayerModel {

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("RETURN"))
    private void ravex$restoreArmSwing(AvatarRenderState state, CallbackInfo ci) {
        if (!Animation.INSTANCE.getEnabled()) return;

        PlayerModel model = (PlayerModel)(Object)this;
        float walkPos = state.walkAnimationPos;
        float walkSpeed = state.walkAnimationSpeed;
        float speedValue = state.speedValue;

        if (speedValue == 0.0f) speedValue = 1.0f;

        float rightBase = Mth.cos(walkPos * 0.6662f + (float)Math.PI) * 2.0f * walkSpeed * 0.5f / speedValue;
        float leftBase  = Mth.cos(walkPos * 0.6662f) * 2.0f * walkSpeed * 0.5f / speedValue;

        model.rightArm.xRot = rightBase;
        model.leftArm.xRot = leftBase;
    }
}
