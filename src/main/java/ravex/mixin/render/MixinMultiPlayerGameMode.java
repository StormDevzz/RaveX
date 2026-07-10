package ravex.mixin.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.event.EventBusHolder;
import ravex.event.combat.AttackEvent;
import ravex.mixin.network.AccessorMultiPlayerGameMode;
import ravex.modules.misc.AntiAttack;
import ravex.modules.misc.BlockMixer;
import ravex.modules.player.ItemSaver;
import ravex.modules.player.PacketMine;
import ravex.modules.render.FreeCam;

@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {

    
    
    
    
    
    @Inject(method = "useItemOn",
            at = @At("RETURN"))
    private void onUseItemOn(LocalPlayer player,
                             InteractionHand hand,
                             BlockHitResult hitResult,
                             CallbackInfoReturnable<InteractionResult> cir) {
        if (!BlockMixer.maybeEnabled()) return;
        if (cir.getReturnValue() == null || !cir.getReturnValue().consumesAction()) return;
        BlockMixer.itz().shuffle();
    }

    
    
    
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onAttack(Player player, net.minecraft.world.entity.Entity target, CallbackInfo ci) {
        AttackEvent event = new AttackEvent(player, target);
        EventBusHolder.get().post(event);
        if (event.isCancelled()) { ci.cancel(); return; }

        if (ItemSaver.itz().shouldSave(player.getMainHandItem())) {
            ci.cancel();
        }
        if (AntiAttack.itz().shouldCancel(target)) {
            ci.cancel();
        }
        if (FreeCam.maybeEnabled() && !FreeCam.itz().entityInteract.getValue()) {
            ci.cancel();
        }
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onStartDestroyBlock(net.minecraft.core.BlockPos pos, net.minecraft.core.Direction face, CallbackInfoReturnable<Boolean> cir) {
        var mc = Minecraft.getInstance();
        if (mc.player != null && ItemSaver.itz().shouldSave(mc.player.getMainHandItem())) {
            cir.setReturnValue(false);
        }
        if (FreeCam.maybeEnabled() && !FreeCam.itz().blockInteract.getValue()) {
            cir.setReturnValue(false);
        }
        ravex.modules.render.Particles.minedThisTick = true;
        ravex.modules.render.Particles.lastMinePos = net.minecraft.world.phys.Vec3.atCenterOf(pos);

        if (PacketMine.maybeEnabled() && "Grim".equals(PacketMine.itz().mode.getValue())) {
            if (PacketMine.itz().isTargetBlock(pos)) {
                ((AccessorMultiPlayerGameMode) this).setDestroyBlockPos(pos);
            }
        }
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onContinueDestroyBlock(net.minecraft.core.BlockPos pos, net.minecraft.core.Direction face, CallbackInfoReturnable<Boolean> cir) {
        var mc = Minecraft.getInstance();
        if (mc.player != null && ItemSaver.itz().shouldSave(mc.player.getMainHandItem())) {
            cir.setReturnValue(false);
        }
        if (FreeCam.maybeEnabled() && !FreeCam.itz().blockInteract.getValue()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void onUseItem(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (ItemSaver.itz().shouldSave(player.getItemInHand(hand))) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void onUseItemOnHead(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (ItemSaver.itz().shouldSave(player.getItemInHand(hand))) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
