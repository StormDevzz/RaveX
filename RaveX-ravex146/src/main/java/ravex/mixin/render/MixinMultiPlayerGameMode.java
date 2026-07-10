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
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.misc.BlockSelector;
import ravex.modules.misc.ItemScroller;
import ravex.modules.exploit.PacketPlace;

@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {




    @Inject(method = "handleInventoryMouseClick",
            at = @At("HEAD"), cancellable = true)
    private void onHandleInventoryMouseClick(int syncId, int slotId, int button, ClickType type,
                                              Player player, CallbackInfo ci) {
        if (!ItemScroller.INSTANCE.getEnabled()) return;
        if (ItemScroller.INSTANCE.isPauseListening()) return;
        if (type != ClickType.THROW) return;

        long window = Minecraft.getInstance().getWindow().handle();
        boolean ctrl  = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL)  == GLFW.GLFW_PRESS
                     || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
        boolean shift = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT)   == GLFW.GLFW_PRESS
                     || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT)  == GLFW.GLFW_PRESS;
        if (!ctrl || !shift) return;

        ci.cancel();
        ItemScroller.INSTANCE.handleClick(slotId);
    }




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
        if (ravex.modules.player.ToolSaver.INSTANCE.shouldSave(player.getMainHandItem())) {
            ci.cancel();
        }
        if (ravex.modules.misc.AntiAttack.INSTANCE.shouldCancel(target)) {
            ci.cancel();
        }
        ravex.modules.misc.Announcer.INSTANCE.onHit();
        ravex.modules.render.Crosshair.INSTANCE.onHit();
        ravex.modules.render.Particles.attackedThisTick = true;
        ravex.modules.render.Particles.lastAttackPos = target.position();
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onStartDestroyBlock(net.minecraft.core.BlockPos pos, net.minecraft.core.Direction face, CallbackInfoReturnable<Boolean> cir) {
        var mc = Minecraft.getInstance();
        if (mc.player != null && ravex.modules.player.ToolSaver.INSTANCE.shouldSave(mc.player.getMainHandItem())) {
            cir.setReturnValue(false);
        }
        ravex.modules.render.Particles.minedThisTick = true;
        ravex.modules.render.Particles.lastMinePos = net.minecraft.world.phys.Vec3.atCenterOf(pos);
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onContinueDestroyBlock(net.minecraft.core.BlockPos pos, net.minecraft.core.Direction face, CallbackInfoReturnable<Boolean> cir) {
        var mc = Minecraft.getInstance();
        if (mc.player != null && ravex.modules.player.ToolSaver.INSTANCE.shouldSave(mc.player.getMainHandItem())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void onUseItem(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (ravex.modules.player.ToolSaver.INSTANCE.shouldSave(player.getItemInHand(hand))) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void onUseItemOnHead(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (ravex.modules.player.ToolSaver.INSTANCE.shouldSave(player.getItemInHand(hand))) {
            cir.setReturnValue(InteractionResult.PASS);
        }
        if (PacketPlace.INSTANCE.shouldIntercept()) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}
