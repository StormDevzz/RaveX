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
import ravex.modules.combat.Hitboxes;
import ravex.modules.misc.PortalGui;
import ravex.modules.misc.RideExploit;
import ravex.modules.movement.Avoid;
import ravex.modules.movement.LiquidControl;
import ravex.modules.movement.NoPush;
import ravex.modules.movement.NoSlow;
import ravex.modules.movement.NoWeb;
import ravex.modules.player.ViewLock;
import ravex.modules.render.ESP;
import ravex.modules.render.FreeCam;
import ravex.modules.render.FreeLook;
import ravex.utility.player.rotation.RotationUtility;
import ravex.modules.Modules;

@Mixin(Entity.class)
public abstract class MixinEntity {
    @Inject(method = "turn(DD)V", at = @At("HEAD"), cancellable = true)
    private void onTurn(double yRot, double xRot, CallbackInfo ci) {
        Entity self = (Entity)(Object)this;
        if (!(self instanceof net.minecraft.client.player.LocalPlayer)) return;

        if (Modules.enabled(ViewLock.class)) {
            boolean lockYaw = Modules.get(ViewLock.class).shouldLockYaw(yRot, xRot);
            boolean lockPitch = Modules.get(ViewLock.class).shouldLockPitch(yRot, xRot);

            if (Modules.get(ViewLock.class).isSmoothMode()) {
                if (lockYaw) {
                    float current = self.getYRot();
                    float target = Modules.get(ViewLock.class).getTargetYaw();
                    float speed = Modules.get(ViewLock.class).getSmoothSpeed();
                    float diff = ((target - current + 540) % 360) - 180;
                    self.setYRot(current + diff * speed);
                }
                if (lockPitch) {
                    float current = self.getXRot();
                    float target = Modules.get(ViewLock.class).getTargetPitch();
                    float speed = Modules.get(ViewLock.class).getSmoothSpeed();
                    float diff = target - current;
                    self.setXRot(current + diff * speed);
                }
                ci.cancel();
                return;
            }

            if (lockYaw && lockPitch) {
                ci.cancel();
            } else {
                float sens = Modules.get(ViewLock.class).getSensitivity();
                float currentYaw = self.getYRot();
                float currentPitch = self.getXRot();
                if (!lockYaw)
                    self.setYRot(RotationUtility.normalizeYaw(currentYaw + (float)yRot * 0.15F * sens));
                if (!lockPitch)
                    self.setXRot(RotationUtility.clampPitch(currentPitch + (float)xRot * 0.15F * sens));
                ci.cancel();
            }
            return;
        }

        if (Modules.enabled(FreeCam.class)) {
            Modules.get(FreeCam.class).turnMixin(yRot * 0.15, xRot * 0.15);
            ci.cancel();
            return;
        }

        if (Modules.enabled(FreeLook.class) && "Camera".equals(Modules.get(FreeLook.class).mode)) {
            Modules.get(FreeLook.class).turn(yRot * 0.15, xRot * 0.15);
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        Entity self = (Entity)(Object)this;
        if (!(self instanceof net.minecraft.client.player.LocalPlayer player)) return;


        if (Modules.enabled(PortalGui.class)) {
            self.portalProcess = null;
        }


        if (Modules.enabled(Avoid.class)) {
            net.minecraft.world.level.Level level = player.level();
            net.minecraft.world.phys.AABB box = player.getBoundingBox().inflate(0.15);
            net.minecraft.core.BlockPos.betweenClosedStream(
                    (int) Math.floor(box.minX), (int) Math.floor(box.minY), (int) Math.floor(box.minZ),
                    (int) Math.floor(box.maxX), (int) Math.floor(box.maxY), (int) Math.floor(box.maxZ)
            ).forEach(blockPos -> {
                net.minecraft.world.level.block.state.BlockState state = level.getBlockState(blockPos);
                if (Modules.get(Avoid.class).shouldAvoid(state.getBlock())) {

                    double bx = blockPos.getX() + 0.5;
                    double bz = blockPos.getZ() + 0.5;
                    double px = player.getX();
                    double pz = player.getZ();
                    double dx = px - bx;
                    double dz = pz - bz;
                    double len = Math.sqrt(dx * dx + dz * dz);
                    if (len < 0.01) { dx = 1; dz = 0; len = 1; }
                    double pushX = (dx / len) * 0.25;
                    double pushZ = (dz / len) * 0.25;
                    player.setPos(player.getX() + pushX, player.getY(), player.getZ() + pushZ);
                }
            });
        }
    }


    @Inject(method = "getBlockSpeedFactor", at = @At("RETURN"), cancellable = true)
    private void onGetBlockSpeedFactor(CallbackInfoReturnable<Float> cir) {
        Entity self = (Entity)(Object)this;
        if (!(self instanceof net.minecraft.client.player.LocalPlayer)) return;

        if (Modules.enabled(NoSlow.class) && Modules.get(NoSlow.class).blocks) {
            cir.setReturnValue(1.0F);
        }
    }

    @Inject(method = "getBlockJumpFactor", at = @At("RETURN"), cancellable = true)
    private void onGetBlockJumpFactor(CallbackInfoReturnable<Float> cir) {
        Entity self = (Entity)(Object)this;
        if (!(self instanceof net.minecraft.client.player.LocalPlayer)) return;

        if (Modules.enabled(NoSlow.class) && Modules.get(NoSlow.class).blocks) {
            cir.setReturnValue(1.0F);
        }
    }

    @Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true)
    private void onMakeStuckInBlock(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.phys.Vec3 motionMultiplier, CallbackInfo ci) {
        Entity self = (Entity)(Object)this;
        if (!(self instanceof net.minecraft.client.player.LocalPlayer)) return;

        if (Modules.enabled(NoWeb.class)) {
            return;
        }
    }

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void onGetTeamColor(CallbackInfoReturnable<Integer> cir) {
        if (Modules.enabled(ESP.class) && Modules.get(ESP.class).mode.equals("Outline")) {
            Entity self = (Entity) (Object) this;
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (self == mc.player) return;

            if (mc.player != null && mc.player.distanceTo(self) > Modules.get(ESP.class).maxDistance) {
                return;
            }

            if (self instanceof Player) {
                if (Modules.get(ESP.class).players) {
                    cir.setReturnValue(Modules.get(ESP.class).playerColor);
                }
            } else if (self instanceof Monster) {
                if (Modules.get(ESP.class).monsters) {
                    cir.setReturnValue(Modules.get(ESP.class).mobColor);
                }
            } else if (self instanceof net.minecraft.world.entity.animal.Animal || self instanceof net.minecraft.world.entity.ambient.AmbientCreature) {
                if (Modules.get(ESP.class).animals) {
                    cir.setReturnValue(Modules.get(ESP.class).animalColor);
                }
            } else if (self instanceof net.minecraft.world.entity.item.ItemEntity) {
                if (Modules.get(ESP.class).items) {
                    cir.setReturnValue(Modules.get(ESP.class).itemColor);
                }
            } else if (self instanceof net.minecraft.world.entity.decoration.ItemFrame) {
                if (Modules.get(ESP.class).frames) {
                    cir.setReturnValue(Modules.get(ESP.class).frameColor);
                }
            }
        }
    }

    @Inject(method = "isLocalClientAuthoritative()Z", at = @At("HEAD"), cancellable = true)
    private void onIsLocalClientAuthoritative(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity)(Object)this;
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null && self.getControllingPassenger() == mc.player) {
            if (Modules.enabled(RideExploit.class)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "getBoundingBox", at = @At("RETURN"), cancellable = true)
    private void onGetBoundingBox(CallbackInfoReturnable<net.minecraft.world.phys.AABB> cir) {
        if (Modules.enabled(Hitboxes.class)) {
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
                    double size = Modules.get(Hitboxes.class).size;
                    if (cir.getReturnValue() != null) {
                        cir.setReturnValue(cir.getReturnValue().inflate(size));
                    }
                }
            }
        }
    }

    @Inject(method = "push(DDD)V", at = @At("HEAD"), cancellable = true)
    private void onPushVelocity(double x, double y, double z, CallbackInfo ci) {
        if (Modules.enabled(NoPush.class)) {
            Entity self = (Entity)(Object)this;
            if (self instanceof net.minecraft.client.player.LocalPlayer) {
                if (Modules.get(NoPush.class).water) {
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void onPushEntity(Entity other, CallbackInfo ci) {
        if (Modules.enabled(NoPush.class)) {
            Entity self = (Entity)(Object)this;
            if (self instanceof net.minecraft.client.player.LocalPlayer) {
                if (Modules.get(NoPush.class).shouldCancelPush(self, other)) {
                    ci.cancel();
                    return;
                }
            }
            if (other instanceof net.minecraft.client.player.LocalPlayer) {
                if (Modules.get(NoPush.class).shouldCancelPush(other, self)) {
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "isInWater()Z", at = @At("HEAD"), cancellable = true)
    private void onIsInWater(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity)(Object)this;
        if (self == net.minecraft.client.Minecraft.getInstance().player) {
            if (Modules.enabled(LiquidControl.class) && Modules.get(LiquidControl.class).water) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "isInLava()Z", at = @At("HEAD"), cancellable = true)
    private void onIsInLava(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity)(Object)this;
        if (self == net.minecraft.client.Minecraft.getInstance().player) {
            if (Modules.enabled(LiquidControl.class) && Modules.get(LiquidControl.class).lava) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "updateInWaterStateAndDoFluidPushing()Z", at = @At("HEAD"), cancellable = true)
    private void onUpdateInWaterStateAndDoFluidPushing(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity)(Object)this;
        if (self == net.minecraft.client.Minecraft.getInstance().player) {
            if (Modules.enabled(LiquidControl.class) && Modules.get(LiquidControl.class).water) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "updateFluidHeightAndDoFluidPushing(Lnet/minecraft/tags/TagKey;D)Z", at = @At("HEAD"), cancellable = true)
    private void onUpdateFluidHeightAndDoFluidPushing(net.minecraft.tags.TagKey<net.minecraft.world.level.material.Fluid> tag, double d, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity)(Object)this;
        if (self == net.minecraft.client.Minecraft.getInstance().player) {
            if (Modules.enabled(LiquidControl.class)) {
                boolean bypassWater = Modules.get(LiquidControl.class).water;
                boolean bypassLava = Modules.get(LiquidControl.class).lava;
                boolean bypassOthers = Modules.get(LiquidControl.class).others;

                if (tag.equals(net.minecraft.tags.FluidTags.WATER) && bypassWater) {
                    cir.setReturnValue(false);
                } else if (tag.equals(net.minecraft.tags.FluidTags.LAVA) && bypassLava) {
                    cir.setReturnValue(false);
                } else if (bypassOthers) {
                    if (!tag.equals(net.minecraft.tags.FluidTags.WATER) && !tag.equals(net.minecraft.tags.FluidTags.LAVA)) {
                        cir.setReturnValue(false);
                    }
                }
            }
        }
    }

    @Inject(method = "isEyeInFluid(Lnet/minecraft/tags/TagKey;)Z", at = @At("HEAD"), cancellable = true)
    private void onIsEyeInFluid(net.minecraft.tags.TagKey<net.minecraft.world.level.material.Fluid> tag, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity)(Object)this;
        if (self == net.minecraft.client.Minecraft.getInstance().player) {
            if (Modules.enabled(LiquidControl.class)) {
                boolean bypassWater = Modules.get(LiquidControl.class).water;
                boolean bypassLava = Modules.get(LiquidControl.class).lava;
                boolean bypassOthers = Modules.get(LiquidControl.class).others;

                if (tag.equals(net.minecraft.tags.FluidTags.WATER) && bypassWater) {
                    cir.setReturnValue(false);
                } else if (tag.equals(net.minecraft.tags.FluidTags.LAVA) && bypassLava) {
                    cir.setReturnValue(false);
                } else if (bypassOthers) {
                    if (!tag.equals(net.minecraft.tags.FluidTags.WATER) && !tag.equals(net.minecraft.tags.FluidTags.LAVA)) {
                        cir.setReturnValue(false);
                    }
                }
            }
        }
    }
}

