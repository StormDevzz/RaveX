package ravex.mixin.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.render.FreeCam;

@Mixin(GameRenderer.class)
public abstract class MixinGameRendererFreeCam {

    @Inject(method = "pick", at = @At("HEAD"))
    private void onPickPre(float tickDelta, CallbackInfo ci) {
        if (!FreeCam.INSTANCE.getEnabled() || !FreeCam.INSTANCE.placeTrace.getValue()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        mc.player.setYRot(FreeCam.INSTANCE.yaw);
        mc.player.setXRot(FreeCam.INSTANCE.pitch);
        FreeCam.Vec3 eye = FreeCam.INSTANCE.getEyePosition(tickDelta);
        mc.player.setPos(eye.x(), eye.y() - mc.player.getEyeHeight(), eye.z());
    }
}
