package ravex.mixin.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.render.ESP;

@Mixin(Entity.class)
public abstract class MixinEntity {
    @Inject(method = "getBlockSpeedFactor", at = @At("RETURN"), cancellable = true)
    private void onGetBlockSpeedFactor(CallbackInfoReturnable<Float> cir) {
        Entity self = (Entity)(Object)this;
        if (!(self instanceof net.minecraft.client.player.LocalPlayer)) return;

        if (ravex.modules.movement.NoSlowDown.INSTANCE.getEnabled() && ravex.modules.movement.NoSlowDown.INSTANCE.blocks.getValue()) {
            cir.setReturnValue(1.0F);
        }
    }

    @Inject(method = "getBlockJumpFactor", at = @At("RETURN"), cancellable = true)
    private void onGetBlockJumpFactor(CallbackInfoReturnable<Float> cir) {
        Entity self = (Entity)(Object)this;
        if (!(self instanceof net.minecraft.client.player.LocalPlayer)) return;

        if (ravex.modules.movement.NoSlowDown.INSTANCE.getEnabled() && ravex.modules.movement.NoSlowDown.INSTANCE.blocks.getValue()) {
            cir.setReturnValue(1.0F);
        }
    }

    @Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true)
    private void onMakeStuckInBlock(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.phys.Vec3 motionMultiplier, CallbackInfo ci) {
        Entity self = (Entity)(Object)this;
        if (!(self instanceof net.minecraft.client.player.LocalPlayer)) return;

        if (ravex.modules.movement.NoWeb.INSTANCE.getEnabled()) {
            return;
        }

        if (ravex.modules.movement.NoSlowDown.INSTANCE.getEnabled() && ravex.modules.movement.NoSlowDown.INSTANCE.cobwebs.getValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void onGetTeamColor(CallbackInfoReturnable<Integer> cir) {
        if (ESP.INSTANCE.getEnabled() && ESP.INSTANCE.mode.getValue().equals("Outline")) {
            Entity self = (Entity) (Object) this;
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (self == mc.player) return;

            if (mc.player != null && mc.player.distanceTo(self) > ESP.INSTANCE.maxDistance.getValue()) {
                return;
            }

            if (self instanceof Player) {
                if (ESP.INSTANCE.players.getValue()) {
                    cir.setReturnValue(ESP.INSTANCE.playerColor.getValue());
                }
            } else if (self instanceof Monster) {
                if (ESP.INSTANCE.monsters.getValue()) {
                    cir.setReturnValue(ESP.INSTANCE.mobColor.getValue());
                }
            } else if (self instanceof net.minecraft.world.entity.animal.Animal || self instanceof net.minecraft.world.entity.ambient.AmbientCreature) {
                if (ESP.INSTANCE.animals.getValue()) {
                    cir.setReturnValue(ESP.INSTANCE.animalColor.getValue());
                }
            } else if (self instanceof net.minecraft.world.entity.item.ItemEntity) {
                if (ESP.INSTANCE.items.getValue()) {
                    cir.setReturnValue(ESP.INSTANCE.itemColor.getValue());
                }
            } else if (self instanceof net.minecraft.world.entity.decoration.ItemFrame) {
                if (ESP.INSTANCE.frames.getValue()) {
                    cir.setReturnValue(ESP.INSTANCE.frameColor.getValue());
                }
            }
        }
    }

    @Inject(method = "isLocalClientAuthoritative()Z", at = @At("HEAD"), cancellable = true)
    private void onIsLocalClientAuthoritative(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity)(Object)this;
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null && self.getControllingPassenger() == mc.player) {
            if (ravex.modules.exploit.RideExploit.INSTANCE.getEnabled()) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "getBoundingBox", at = @At("RETURN"), cancellable = true)
    private void onGetBoundingBox(CallbackInfoReturnable<net.minecraft.world.phys.AABB> cir) {
        if (ravex.modules.combat.Hitboxes.INSTANCE.getEnabled()) {
            Entity self = (Entity)(Object)this;
            if (self != net.minecraft.client.Minecraft.getInstance().player && self instanceof LivingEntity) {
                boolean isRaytracing = false;
                for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
                    String className = element.getClassName();
                    String methodName = element.getMethodName();
                    if (className.contains("ProjectileUtil") || className.contains("GameRenderer") || methodName.contains("pick")) {
                        isRaytracing = true;
                        break;
                    }
                }
                if (isRaytracing) {
                    double size = ravex.modules.combat.Hitboxes.INSTANCE.size.getValue();
                    if (cir.getReturnValue() != null) {
                        cir.setReturnValue(cir.getReturnValue().inflate(size));
                    }
                }
            }
        }
    }

    @Inject(method = "push(DDD)V", at = @At("HEAD"), cancellable = true)
    private void onPushVelocity(double x, double y, double z, CallbackInfo ci) {
        if (ravex.modules.movement.NoPush.INSTANCE.getEnabled()) {
            Entity self = (Entity)(Object)this;
            if (self instanceof net.minecraft.client.player.LocalPlayer) {
                if (ravex.modules.movement.NoPush.INSTANCE.water.getValue()) {
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void onPushEntity(Entity other, CallbackInfo ci) {
        if (ravex.modules.movement.NoPush.INSTANCE.getEnabled()) {
            Entity self = (Entity)(Object)this;
            if (self instanceof net.minecraft.client.player.LocalPlayer) {
                if (ravex.modules.movement.NoPush.INSTANCE.shouldCancelPush(self, other)) {
                    ci.cancel();
                }
            }
        }
    }
}
