package ravex.mixin.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.event.EventBusHolder;
import ravex.event.combat.AttackEvent;
import ravex.mixin.network.AccessorMultiPlayerGameMode;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.utility.misc.PhysicUtility;
import ravex.utility.player.InventoryUtility;
import ravex.modules.misc.AntiAttack;
import ravex.modules.misc.BlockMixer;
import ravex.modules.player.ItemSaver;
import ravex.modules.player.PacketMine;
import ravex.modules.render.FreeCam;
import ravex.modules.Modules;

@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {






    @Inject(method = "useItemOn",
            at = @At("RETURN"))
    private void onUseItemOn(LocalPlayer player,
                             InteractionHand hand,
                             BlockHitResult hitResult,
                             CallbackInfoReturnable<InteractionResult> cir) {
        if (!Modules.enabled(BlockMixer.class)) return;
        if (cir.getReturnValue() == null || !cir.getReturnValue().consumesAction()) return;
        Modules.get(BlockMixer.class).shuffle();
    }




    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void onAttack(Player player, net.minecraft.world.entity.Entity target, CallbackInfo ci) {
        AttackEvent event = new AttackEvent(player, target);
        EventBusHolder.get().post(event);
        if (event.isCancelled()) { ci.cancel(); return; }

        if (Modules.get(ItemSaver.class).shouldSave(InventoryUtility.getMainHand(player))) {
            ci.cancel();
        }
        if (Modules.get(AntiAttack.class).shouldCancel(target)) {
            ci.cancel();
        }
        if (Modules.enabled(FreeCam.class) && !Modules.get(FreeCam.class).entityInteract) {
            ci.cancel();
        }
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onStartDestroyBlock(net.minecraft.core.BlockPos pos, net.minecraft.core.Direction face, CallbackInfoReturnable<Boolean> cir) {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() != null && Modules.get(ItemSaver.class).shouldSave(InventoryUtility.getMainHand(mc.getPlayer()))) {
            cir.setReturnValue(false);
        }
        if (Modules.enabled(FreeCam.class) && !Modules.get(FreeCam.class).blockInteract) {
            cir.setReturnValue(false);
        }
        ravex.modules.render.Particles.minedThisTick = true;
        ravex.modules.render.Particles.lastMinePos = PhysicUtility.atCenterOf(pos);

        if (Modules.enabled(PacketMine.class) && "Grim".equals(Modules.get(PacketMine.class).mode)) {
            if (Modules.get(PacketMine.class).isTargetBlock(pos)) {
                ((AccessorMultiPlayerGameMode) this).setDestroyBlockPos(pos);
            }
        }
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onContinueDestroyBlock(net.minecraft.core.BlockPos pos, net.minecraft.core.Direction face, CallbackInfoReturnable<Boolean> cir) {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() != null && Modules.get(ItemSaver.class).shouldSave(InventoryUtility.getMainHand(mc.getPlayer()))) {
            cir.setReturnValue(false);
        }
        if (Modules.enabled(FreeCam.class) && !Modules.get(FreeCam.class).blockInteract) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void onUseItem(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (Modules.get(ItemSaver.class).shouldSave(player.getItemInHand(hand))) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void onUseItemOnHead(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (Modules.get(ItemSaver.class).shouldSave(player.getItemInHand(hand))) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
