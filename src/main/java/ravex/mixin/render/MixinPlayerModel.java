package ravex.mixin.render;

import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.render.Animations;

@Mixin(value = PlayerModel.class, priority = 9999)
public class MixinPlayerModel {

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("RETURN"))
    private void ravex$applyAnimations(AvatarRenderState state, CallbackInfo ci) {
        if (!Animations.maybeEnabled()) return;

        PlayerModel model = (PlayerModel) (Object) this;
        Animations anim = Animations.itz();
        String mode = anim.mode.getValue();
        float speed = anim.swingSpeed.getValue().floatValue();

        if (mode.equals("Dortware")) {
            float time = (float) (System.currentTimeMillis() / 1000.0);
            float swingPhase = time * speed * 0.5f;
            float armSwing = Mth.sin(swingPhase) * 0.3f;
            float armSwing2 = Mth.sin(swingPhase + 1.5f) * 0.2f;

            model.rightArm.xRot += armSwing;
            model.leftArm.xRot += armSwing2;
            model.rightArm.zRot -= 0.1f + Mth.sin(swingPhase * 2f) * 0.05f;
            model.leftArm.zRot += 0.1f + Mth.cos(swingPhase * 2f) * 0.05f;
        }
    }
}
