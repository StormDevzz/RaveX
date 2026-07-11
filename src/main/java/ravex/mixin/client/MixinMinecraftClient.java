package ravex.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.RaveX;
import ravex.modules.misc.AntiQuit;
import ravex.modules.render.ESP;
import ravex.modules.render.FreeCam;
import ravex.modules.world.Scaffold;

@Mixin(Minecraft.class)
public abstract class MixinMinecraftClient {
    @Unique private static double ravexFreeCamSavedX, ravexFreeCamSavedY, ravexFreeCamSavedZ;
    @Unique private static float ravexFreeCamSavedYaw, ravexFreeCamSavedPitch;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickHead(CallbackInfo ci) {
        ravex.modules.combat.KillAura.onPreTick();
        Scaffold.onPreTick();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        RaveX.onClientTick();
    }

    @Inject(method = "disconnectFromWorld", at = @At("HEAD"), cancellable = true)
    private void onDisconnectFromWorld(net.minecraft.network.chat.Component component, CallbackInfo ci) {
        if (AntiQuit.shouldBlockDisconnect()) {
            ci.cancel();
        }
    }

    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void onShouldEntityAppearGlowing(net.minecraft.world.entity.Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (ESP.shouldGlow(entity)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void onStartAttackPre(CallbackInfoReturnable<Boolean> cir) {
        if (!FreeCam.maybeEnabled()) return;
        if (!FreeCam.itz().blockInteract.getValue() && !FreeCam.itz().entityInteract.getValue()) {
            cir.setReturnValue(false);
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        ravexFreeCamSavedX = player.getX();
        ravexFreeCamSavedY = player.getY();
        ravexFreeCamSavedZ = player.getZ();
        ravexFreeCamSavedYaw = player.getYRot();
        ravexFreeCamSavedPitch = player.getXRot();

        player.setPos(FreeCam.itz().x, FreeCam.itz().y - player.getEyeHeight(), FreeCam.itz().z);
        player.setYRot(FreeCam.itz().yaw);
        player.setXRot(FreeCam.itz().pitch);
    }

    @Inject(method = "startAttack", at = @At("RETURN"))
    private void onStartAttackPost(CallbackInfoReturnable<Boolean> cir) {
        if (!FreeCam.maybeEnabled()) return;
        if (!FreeCam.itz().blockInteract.getValue() && !FreeCam.itz().entityInteract.getValue()) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        player.setPos(ravexFreeCamSavedX, ravexFreeCamSavedY, ravexFreeCamSavedZ);
        player.setYRot(ravexFreeCamSavedYaw);
        player.setXRot(ravexFreeCamSavedPitch);
    }
}
