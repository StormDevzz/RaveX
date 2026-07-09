package ravex.mixin.render;

import net.minecraft.client.Camera;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.event.EventBusHolder;
import ravex.event.render.CameraEvent;
import ravex.modules.render.FreeLook;
import ravex.modules.render.FreeCam;
import ravex.modules.render.ViewClip;

@Mixin(Camera.class)
public abstract class MixinCamera {

    @Shadow private float yRot;
    @Shadow private float xRot;
    @Shadow private net.minecraft.world.phys.Vec3 position;

    @Shadow protected abstract float getMaxZoom(float startingDistance);
    @Shadow protected abstract void setRotation(float yRot, float xRot);
    @Shadow protected abstract void setPosition(net.minecraft.world.phys.Vec3 pos);


<<<<<<< HEAD
    @Inject(method = "setup", at = @At("HEAD"), cancellable = true)
    private void onSetupCamera(Level area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        CameraEvent event = new CameraEvent(CameraEvent.CameraMode.NORMAL, position.x, position.y, position.z, yRot, xRot);
        EventBusHolder.get().post(event);
        if (event.isCancelled()) { ci.cancel(); return; }
    }

=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    @Inject(method = "setup", at = @At("RETURN"))
    private void onSetup(Level area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (FreeCam.maybeEnabled()) {
            double[] coords = FreeCam.itz().getCorrectedRenderCoordinates(tickDelta);
            net.minecraft.world.phys.Vec3 targetPos = new net.minecraft.world.phys.Vec3(coords[0], coords[1], coords[2]);
            this.setPosition(targetPos);
            this.setRotation((float) coords[3], (float) coords[4]);
        } else if (FreeLook.maybeEnabled()) {
            float yaw = FreeLook.itz().getLookYaw();
            float pitch = FreeLook.itz().getLookPitch();

            
            double renderX = focusedEntity.xo + (focusedEntity.getX() - focusedEntity.xo) * tickDelta;
            double renderY = focusedEntity.yo + (focusedEntity.getY() - focusedEntity.yo) * tickDelta;
            double renderZ = focusedEntity.zo + (focusedEntity.getZ() - focusedEntity.zo) * tickDelta;
            double eyeHeight = focusedEntity.getEyeHeight();
            net.minecraft.world.phys.Vec3 eyePos = new net.minecraft.world.phys.Vec3(renderX, renderY + eyeHeight, renderZ);

            float f = yaw * ((float)Math.PI / 180F);
            float g = pitch * ((float)Math.PI / 180F);
            float cosPitch = (float) Math.cos(g);
            float sinPitch = (float) Math.sin(g);
            float cosYaw = (float) Math.cos(f);
            float sinYaw = (float) Math.sin(f);

            net.minecraft.world.phys.Vec3 dirVec = new net.minecraft.world.phys.Vec3(
                -sinYaw * cosPitch,
                -sinPitch,
                cosYaw * cosPitch
            );

            
<<<<<<< HEAD
            float startingDist = ViewClip.maybeEnabled() ? ViewClip.itz().cameraDistance.getValue().floatValue() : 4.0f;
=======
            float startingDist = ViewClip.INSTANCE.getEnabled() ? ViewClip.INSTANCE.cameraDistance.getValue().floatValue() : 4.0f;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            float zoom = getMaxZoom(startingDist);
            net.minecraft.world.phys.Vec3 targetPos = eyePos.subtract(dirVec.scale(zoom));
            this.setPosition(targetPos);
            this.setRotation(yaw, pitch);
        }

    }

    @Inject(method = "isDetached", at = @At("HEAD"), cancellable = true)
    private void onIsDetached(CallbackInfoReturnable<Boolean> cir) {
        
<<<<<<< HEAD
        if (FreeCam.maybeEnabled()) {
=======
        if (FreeCam.INSTANCE.getEnabled()) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            cir.setReturnValue(true);
        }
    }

    private boolean inGetMaxZoom = false;

    @Inject(method = "getMaxZoom", at = @At("HEAD"), cancellable = true)
    private void onGetMaxZoom(float startingDistance, CallbackInfoReturnable<Float> cir) {
        if (inGetMaxZoom) return;
        if (ViewClip.maybeEnabled()) {
            float dist = ViewClip.itz().cameraDistance.getValue().floatValue();
            if (ViewClip.itz().bypassWalls.getValue()) {
                cir.setReturnValue(dist);
            } else {
                inGetMaxZoom = true;
                try {
                    cir.setReturnValue(this.getMaxZoom(dist));
                } finally {
                    inGetMaxZoom = false;
                }
            }
        }
    }
}
