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
<<<<<<< HEAD
        if (!FreeCam.maybeEnabled() || !FreeCam.itz().placeTrace.getValue()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        mc.player.setYRot(FreeCam.itz().yaw);
        mc.player.setXRot(FreeCam.itz().pitch);
        FreeCam.Vec3 eye = FreeCam.itz().getEyePosition(tickDelta);
=======
        if (!FreeCam.INSTANCE.getEnabled() || !FreeCam.INSTANCE.placeTrace.getValue()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        mc.player.setYRot(FreeCam.INSTANCE.yaw);
        mc.player.setXRot(FreeCam.INSTANCE.pitch);
        FreeCam.Vec3 eye = FreeCam.INSTANCE.getEyePosition(tickDelta);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        mc.player.setPos(eye.x(), eye.y() - mc.player.getEyeHeight(), eye.z());
    }
}
