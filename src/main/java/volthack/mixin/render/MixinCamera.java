package volthack.mixin.render;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class MixinCamera {
    @Unique
    private float voltHack_origYaw;
    @Unique
    private float voltHack_origPitch;
    @Unique
    private float voltHack_origYawO;
    @Unique
    private float voltHack_origPitchO;

    @Inject(method = "setup", at = @At("HEAD"))
    private void onSetupHead(Level level, Entity focusedEntity, boolean bl, boolean bl2, float f, CallbackInfo ci) {
        if (!volthack.modules.render.FreeLook.INSTANCE.getEnabled()) return;
        if (focusedEntity != Minecraft.getInstance().player) return;

        voltHack_origYaw = focusedEntity.getYRot();
        voltHack_origPitch = focusedEntity.getXRot();
        voltHack_origYawO = focusedEntity.yRotO;
        voltHack_origPitchO = focusedEntity.xRotO;

        float freeYaw = volthack.modules.render.FreeLook.INSTANCE.getFreeYaw();
        float freePitch = volthack.modules.render.FreeLook.INSTANCE.getFreePitch();
        focusedEntity.setYRot(freeYaw);
        focusedEntity.setXRot(freePitch);
        focusedEntity.yRotO = freeYaw;
        focusedEntity.xRotO = freePitch;
    }

    @Inject(method = "setup", at = @At("TAIL"))
    private void onSetupTail(Level level, Entity focusedEntity, boolean bl, boolean bl2, float f, CallbackInfo ci) {
        if (!volthack.modules.render.FreeLook.INSTANCE.getEnabled()) return;
        if (focusedEntity != Minecraft.getInstance().player) return;

        focusedEntity.setYRot(voltHack_origYaw);
        focusedEntity.setXRot(voltHack_origPitch);
        focusedEntity.yRotO = voltHack_origYawO;
        focusedEntity.xRotO = voltHack_origPitchO;
    }

    @Inject(method = "getMaxZoom", at = @At("HEAD"), cancellable = true)
    private void onGetMaxZoom(float startingDistance, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Float> cir) {
        if (volthack.modules.render.ViewClip.INSTANCE.getEnabled()) {
            cir.setReturnValue(startingDistance);
        }
    }
}
