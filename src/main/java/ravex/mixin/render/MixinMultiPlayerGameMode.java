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
import ravex.modules.misc.BlockSelector;

@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {

    
    
    
    
    
    @Inject(method = "useItemOn",
            at = @At("RETURN"))
    private void onUseItemOn(LocalPlayer player,
                             InteractionHand hand,
                             BlockHitResult hitResult,
                             CallbackInfoReturnable<InteractionResult> cir) {
        if (!BlockSelector.INSTANCE.getEnabled()) return;
        if (cir.getReturnValue() != InteractionResult.CONSUME
         && cir.getReturnValue() != InteractionResult.SUCCESS) return;
        BlockSelector.INSTANCE.selectRandomBlock();
    }

    
    
    
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onAttack(Player player, net.minecraft.world.entity.Entity target, CallbackInfo ci) {
        if (ravex.modules.player.ItemSaver.INSTANCE.shouldSave(player.getMainHandItem())) {
            ci.cancel();
        }
        if (ravex.modules.misc.AntiAttack.INSTANCE.shouldCancel(target)) {
            ci.cancel();
        }
        ravex.modules.misc.ChatUtils.INSTANCE.onHit();
        ravex.modules.render.Crosshair.INSTANCE.onHit();
        ravex.modules.render.Particles.attackedThisTick = true;
        ravex.modules.render.Particles.lastAttackPos = target.position();
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onStartDestroyBlock(net.minecraft.core.BlockPos pos, net.minecraft.core.Direction face, CallbackInfoReturnable<Boolean> cir) {
        var mc = Minecraft.getInstance();
        if (mc.player != null && ravex.modules.player.ItemSaver.INSTANCE.shouldSave(mc.player.getMainHandItem())) {
            cir.setReturnValue(false);
        }
        ravex.modules.render.Particles.minedThisTick = true;
        ravex.modules.render.Particles.lastMinePos = net.minecraft.world.phys.Vec3.atCenterOf(pos);
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onContinueDestroyBlock(net.minecraft.core.BlockPos pos, net.minecraft.core.Direction face, CallbackInfoReturnable<Boolean> cir) {
        var mc = Minecraft.getInstance();
        if (mc.player != null && ravex.modules.player.ItemSaver.INSTANCE.shouldSave(mc.player.getMainHandItem())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void onUseItem(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (ravex.modules.player.ItemSaver.INSTANCE.shouldSave(player.getItemInHand(hand))) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void onUseItemOnHead(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (ravex.modules.player.ItemSaver.INSTANCE.shouldSave(player.getItemInHand(hand))) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
