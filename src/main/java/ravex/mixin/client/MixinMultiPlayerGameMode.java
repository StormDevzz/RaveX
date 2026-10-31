package ravex.mixin.client;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.mixin.network.AccessorMultiPlayerGameMode;
import ravex.modules.player.PacketMine;
import ravex.modules.Modules;

@Mixin(MultiPlayerGameMode.class)
public class MixinMultiPlayerGameMode {
    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onStartDestroyBlock(BlockPos pos, Direction facing, CallbackInfoReturnable<Boolean> cir) {
        PacketMine pm = Modules.get(PacketMine.class);

        if (Modules.enabled(PacketMine.class) && pm.grimMode.equals("Strict")) {
            if (pm.isTargetBlock(pos)) {
                AccessorMultiPlayerGameMode accessor = (AccessorMultiPlayerGameMode) this;
                accessor.setDestroyBlockPos(pos);
            }
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void onUseItemOn(net.minecraft.client.player.LocalPlayer player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit, CallbackInfoReturnable<net.minecraft.world.InteractionResult> cir) {
        if (!Modules.enabled(ravex.modules.misc.StashFinder.class)) return;
        ravex.modules.misc.StashFinder.rememberContainer(player.level().getBlockState(hit.getBlockPos()), hit.getBlockPos());
    }

    @Inject(method = "handleInventoryMouseClick", at = @At("HEAD"), cancellable = true)
    private void onHandleInventoryMouseClick(int containerId, int slotId, int mouseButton,
                                             net.minecraft.world.inventory.ClickType clickType,
                                             net.minecraft.world.entity.player.Player player, CallbackInfo ci) {
        if (!Modules.enabled(ravex.modules.movement.GuiMove.class)) return;
        var mc = ravex.mcwrapper.MinecraftWrapper.getInstance();
        if (mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen
                && player.containerMenu == screen.getMenu()
                && !player.containerMenu.stillValid(player)) {
            // out of range: avoids ghost items
            ci.cancel();
        }
    }

    // fastBreak: vanilla waits 5 ticks between blocks
    @Inject(method = "tick", at = @At("HEAD"))
    private void ravexFastBreakTick(CallbackInfo ci) {
        if (!Modules.enabled(ravex.modules.player.FastBreak.class)) return;
        ravex.modules.player.FastBreak fb = Modules.get(ravex.modules.player.FastBreak.class);
        if (fb == null) return;
        ((AccessorMultiPlayerGameMode) this).setDestroyDelay((int) fb.delay);
    }
}
