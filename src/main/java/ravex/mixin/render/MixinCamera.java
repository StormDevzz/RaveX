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
import ravex.modules.Modules;

@Mixin(Camera.class)
public abstract class MixinCamera {

    @Shadow private float yRot;
    @Shadow private float xRot;
    @Shadow private net.minecraft.world.phys.Vec3 position;

    @Shadow protected abstract float getMaxZoom(float startingDistance);
    @Shadow protected abstract void setRotation(float yRot, float xRot);
    @Shadow protected abstract void setPosition(net.minecraft.world.phys.Vec3 pos);


    @Inject(method = "setup", at = @At("HEAD"), cancellable = true)
    private void onSetupCamera(Level area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        CameraEvent event = new CameraEvent(CameraEvent.CameraMode.NORMAL, position.x, position.y, position.z, yRot, xRot);
        EventBusHolder.get().post(event);
        if (event.isCancelled()) { ci.cancel(); return; }
    }

    @Inject(method = "setup", at = @At("RETURN"))
    private void onSetup(Level area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (Modules.enabled(FreeCam.class)) {
            double[] coords = Modules.get(FreeCam.class).getCorrectedRenderCoordinates(tickDelta);
            net.minecraft.world.phys.Vec3 targetPos = new net.minecraft.world.phys.Vec3(coords[0], coords[1], coords[2]);
            this.setPosition(targetPos);
            this.setRotation((float) coords[3], (float) coords[4]);
        } else if (Modules.enabled(FreeLook.class)) {
            float yaw = Modules.get(FreeLook.class).getLookYaw();
            float pitch = Modules.get(FreeLook.class).getLookPitch();


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


            FreeLook flMod = Modules.get(FreeLook.class);
            float startingDist;
            if (flMod != null && flMod.zoom) {
                startingDist = flMod.getCameraDistance();
            } else if (Modules.enabled(ViewClip.class)) {
                startingDist = (float) Modules.get(ViewClip.class).cameraDistance;
            } else {
                startingDist = 4.0f;
            }
            float zoom = getMaxZoom(startingDist);
            net.minecraft.world.phys.Vec3 targetPos = eyePos.subtract(dirVec.scale(zoom));
            this.setPosition(targetPos);
            this.setRotation(yaw, pitch);
        } else if (Modules.enabled(ravex.modules.world.Scaffold.class)
                && ravex.modules.world.Scaffold.noRotateView()
                && ravex.modules.world.Scaffold.capturingView) {
            this.setRotation(ravex.modules.world.Scaffold.viewYaw, ravex.modules.world.Scaffold.viewPitch);
        } else if (Modules.enabled(ravex.modules.world.ECFarmer.class)
                && ravex.modules.world.ECFarmer.capturingView) {
            this.setRotation(ravex.modules.world.ECFarmer.viewYaw, ravex.modules.world.ECFarmer.viewPitch);
        }

        // smooth camera glide on teleports
        boolean scriptOwned = Modules.enabled(FreeCam.class) || Modules.enabled(FreeLook.class)
                || (Modules.enabled(ravex.modules.world.Scaffold.class)
                    && ravex.modules.world.Scaffold.noRotateView()
                    && ravex.modules.world.Scaffold.capturingView)
                || (Modules.enabled(ravex.modules.world.ECFarmer.class)
                    && ravex.modules.world.ECFarmer.capturingView);
        if (!scriptOwned && ravex.modules.render.Animations.smoothCamera()) {
            net.minecraft.world.phys.Vec3 desired = this.position;
            // own clock, shared deltaTime is consumed first
            long now = System.nanoTime();
            float dt = Math.min(50f, (now - smoothLastNanos) / 1_000_000f) / 1000f;
            smoothLastNanos = now;
            // dimension hop: snap instead of gliding
            if (smoothCamPos == null || smoothDim == null || !smoothDim.equals(area.dimension())) {
                smoothCamPos = desired;
                smoothDim = area.dimension();
            } else {
                double dist = smoothCamPos.distanceTo(desired);
                // snap on teleport, small hops still glide
                if (dist > 48.0) {
                    smoothCamPos = desired;
                } else if (dist < 0.004) {
                    smoothCamPos = desired;
                } else {
                    // short distances follow fast, teleports glide long
                    double speed = Math.max(6.0, Math.min(55.0, 55.0 / (1.0 + dist)));
                    float alpha = (float) (1.0 - Math.exp(-dt * speed));
                    smoothCamPos = smoothCamPos.lerp(desired, alpha);
                    if (smoothCamPos.distanceTo(desired) < 0.01) smoothCamPos = desired;
                    this.setPosition(smoothCamPos);
                }
            }
        } else {
            smoothCamPos = null;
            smoothDim = null;
            smoothLastNanos = System.nanoTime();
        }
    }

    @Inject(method = "isDetached", at = @At("HEAD"), cancellable = true)
    private void onIsDetached(CallbackInfoReturnable<Boolean> cir) {

        if (Modules.enabled(FreeCam.class)) {
            cir.setReturnValue(true);
        }
    }

    private boolean inGetMaxZoom = false;
    private net.minecraft.world.phys.Vec3 smoothCamPos;
    private net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> smoothDim = null;
    private long smoothLastNanos = System.nanoTime();

    @Inject(method = "getMaxZoom", at = @At("HEAD"), cancellable = true)
    private void onGetMaxZoom(float startingDistance, CallbackInfoReturnable<Float> cir) {
        if (inGetMaxZoom) return;
        // free look owns distance while zooming
        FreeLook fl = Modules.get(FreeLook.class);
        if (fl != null && Modules.enabled(FreeLook.class) && fl.zoom) {
            cir.setReturnValue(startingDistance);
            return;
        }
        if (Modules.enabled(ViewClip.class)) {
            float dist = (float) Modules.get(ViewClip.class).cameraDistance;
            if (Modules.get(ViewClip.class).bypassWalls) {
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
