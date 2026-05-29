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

    @Inject(method = "setup", at = @At("RETURN"))
    private void onSetup(Level area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (FreeCam.INSTANCE.getEnabled()) {
            double[] coords = FreeCam.INSTANCE.getCorrectedRenderCoordinates(tickDelta);
            net.minecraft.world.phys.Vec3 targetPos = new net.minecraft.world.phys.Vec3(coords[0], coords[1], coords[2]);
            this.setPosition(targetPos);
            this.setRotation((float) coords[3], (float) coords[4]);
        } else if (FreeLook.INSTANCE.getEnabled()) {
            float yaw = FreeLook.INSTANCE.getLookYaw();
            float pitch = FreeLook.INSTANCE.getLookPitch();

            // Buttery smooth camera focus target interpolation to prevent jittering when moving!
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

            // Dynamically calculate non-clipping safe distance using shadowed getMaxZoom method!
            float startingDist = ViewClip.INSTANCE.getEnabled() ? ViewClip.INSTANCE.cameraDistance.getValue().floatValue() : 4.0f;
            float zoom = getMaxZoom(startingDist);
            net.minecraft.world.phys.Vec3 targetPos = eyePos.subtract(dirVec.scale(zoom));
            this.setPosition(targetPos);
            this.setRotation(yaw, pitch);
        }
    }

    @Inject(method = "isDetached", at = @At("HEAD"), cancellable = true)
    private void onIsDetached(CallbackInfoReturnable<Boolean> cir) {
        // Force the camera detached state to true when FreeCam is enabled, which renders the player body!
        if (FreeCam.INSTANCE.getEnabled()) {
            cir.setReturnValue(true);
        }
    }

    private boolean inGetMaxZoom = false;

    @Inject(method = "getMaxZoom", at = @At("HEAD"), cancellable = true)
    private void onGetMaxZoom(float startingDistance, CallbackInfoReturnable<Float> cir) {
        if (inGetMaxZoom) return;
        if (ViewClip.INSTANCE.getEnabled()) {
            float dist = ViewClip.INSTANCE.cameraDistance.getValue().floatValue();
            if (ViewClip.INSTANCE.bypassWalls.getValue()) {
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
