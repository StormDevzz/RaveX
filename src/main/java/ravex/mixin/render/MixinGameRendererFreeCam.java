package ravex.mixin.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.render.FreeCam;

@Mixin(GameRenderer.class)
public abstract class MixinGameRendererFreeCam {
    @Unique private static double ravexSavedX, ravexSavedY, ravexSavedZ;
    @Unique private static float ravexSavedYaw, ravexSavedPitch;

    @Inject(method = "pick", at = @At("HEAD"))
    private void onPickPre(float tickDelta, CallbackInfo ci) {
        if (!FreeCam.maybeEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        ravexSavedX = mc.player.getX();
        ravexSavedY = mc.player.getY();
        ravexSavedZ = mc.player.getZ();
        ravexSavedYaw = mc.player.getYRot();
        ravexSavedPitch = mc.player.getXRot();

        mc.player.setYRot(FreeCam.itz().yaw);
        mc.player.setXRot(FreeCam.itz().pitch);
        FreeCam.Vec3 eye = FreeCam.itz().getEyePosition(tickDelta);
        mc.player.setPos(eye.x(), eye.y() - mc.player.getEyeHeight(), eye.z());
    }

    @Inject(method = "pick", at = @At("RETURN"))
    private void onPickPost(float tickDelta, CallbackInfo ci) {
        if (!FreeCam.maybeEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        mc.player.setPos(ravexSavedX, ravexSavedY, ravexSavedZ);
        mc.player.setYRot(ravexSavedYaw);
        mc.player.setXRot(ravexSavedPitch);
    }
}
