package ravex.mixin.render;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.render.FreeLook;
import ravex.modules.render.FreeCam;

@Mixin(Camera.class)
public abstract class MixinCamera {

    @Shadow private float yRot;
    @Shadow private float xRot;
    @Shadow private net.minecraft.world.phys.Vec3 position;

    @Shadow protected abstract float getMaxZoom(float startingDistance);

    @Inject(method = "setup", at = @At("RETURN"))
    private void onSetup(Level area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (FreeCam.INSTANCE.getEnabled()) {
            double[] coords = FreeCam.INSTANCE.getCorrectedRenderCoordinates(tickDelta);
            this.position = new net.minecraft.world.phys.Vec3(coords[0], coords[1], coords[2]);
            this.yRot = (float) coords[3];
            this.xRot = (float) coords[4];
        } else if (FreeLook.INSTANCE.getEnabled()) {
            this.yRot = FreeLook.INSTANCE.getLookYaw();
            this.xRot = FreeLook.INSTANCE.getLookPitch();

            // Buttery smooth camera focus target interpolation to prevent jittering when moving!
            double renderX = focusedEntity.xo + (focusedEntity.getX() - focusedEntity.xo) * tickDelta;
            double renderY = focusedEntity.yo + (focusedEntity.getY() - focusedEntity.yo) * tickDelta;
            double renderZ = focusedEntity.zo + (focusedEntity.getZ() - focusedEntity.zo) * tickDelta;
            double eyeHeight = focusedEntity.getEyeHeight();
            net.minecraft.world.phys.Vec3 eyePos = new net.minecraft.world.phys.Vec3(renderX, renderY + eyeHeight, renderZ);

            float f = this.yRot * ((float)Math.PI / 180F);
            float g = this.xRot * ((float)Math.PI / 180F);
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
            float zoom = getMaxZoom(4.0f);
            this.position = eyePos.subtract(dirVec.scale(zoom));
        }
    }

    @Inject(method = "isDetached", at = @At("HEAD"), cancellable = true)
    private void onIsDetached(CallbackInfoReturnable<Boolean> cir) {
        // Force the camera detached state to true when FreeCam is enabled, which renders the player body!
        if (FreeCam.INSTANCE.getEnabled()) {
            cir.setReturnValue(true);
        }
    }
}
