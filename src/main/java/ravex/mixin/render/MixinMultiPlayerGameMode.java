package ravex.mixin.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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

@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {

    // =========================================================================
    // ItemScroller — Ctrl+Shift+Throw mass-drop like ThunderHack
    // =========================================================================
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

    // =========================================================================
    // BlockSelector — after placing a block, switch to a random block in hotbar
    // =========================================================================
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
}
