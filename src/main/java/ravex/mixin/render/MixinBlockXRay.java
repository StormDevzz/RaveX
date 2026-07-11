package ravex.mixin.render;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.player.Xray;

@Mixin(BlockBehaviour.BlockStateBase.class)
public class MixinBlockXRay {



    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
    private void onGetRenderShape(CallbackInfoReturnable<RenderShape> cir) {
        if (!Xray.maybeEnabled()) return;
        BlockBehaviour.BlockStateBase self = (BlockBehaviour.BlockStateBase)(Object)this;
        if (!Xray.itz().isBlockSelected(self.getBlock())) {
            cir.setReturnValue(RenderShape.INVISIBLE);
        }
    }



    @Inject(method = "skipRendering", at = @At("HEAD"), cancellable = true)
    private void onSkipRendering(BlockState adjacent, Direction dir, CallbackInfoReturnable<Boolean> cir) {
        if (!Xray.maybeEnabled()) return;
        BlockBehaviour.BlockStateBase self = (BlockBehaviour.BlockStateBase)(Object)this;
        if (Xray.itz().isBlockSelected(self.getBlock())) {

            cir.setReturnValue(false);
        } else {

            cir.setReturnValue(true);
        }
    }









    @Inject(method = "getOcclusionShape", at = @At("HEAD"), cancellable = true)
    private void onGetOcclusionShape(CallbackInfoReturnable<VoxelShape> cir) {
        if (!Xray.maybeEnabled()) return;
        BlockBehaviour.BlockStateBase self = (BlockBehaviour.BlockStateBase)(Object)this;
        if (!Xray.itz().isBlockSelected(self.getBlock())) {
            cir.setReturnValue(Shapes.empty());
        }
    }

    @Inject(method = "getFaceOcclusionShape", at = @At("HEAD"), cancellable = true)
    private void onGetFaceOcclusionShape(Direction direction, CallbackInfoReturnable<VoxelShape> cir) {
        if (!Xray.maybeEnabled()) return;
        BlockBehaviour.BlockStateBase self = (BlockBehaviour.BlockStateBase)(Object)this;
        if (!Xray.itz().isBlockSelected(self.getBlock())) {
            cir.setReturnValue(Shapes.empty());
        }
    }



    @Inject(method = "isSolidRender", at = @At("HEAD"), cancellable = true)
    private void onIsSolidRender(CallbackInfoReturnable<Boolean> cir) {
        if (!Xray.maybeEnabled()) return;
        BlockBehaviour.BlockStateBase self = (BlockBehaviour.BlockStateBase)(Object)this;
        if (!Xray.itz().isBlockSelected(self.getBlock())) {
            cir.setReturnValue(false);
        }
    }



    @Inject(method = "getLightBlock", at = @At("HEAD"), cancellable = true)
    private void onGetLightBlock(CallbackInfoReturnable<Integer> cir) {
        if (Xray.maybeEnabled()) {
            cir.setReturnValue(0);
        }
    }

    @Inject(method = "getLightEmission", at = @At("HEAD"), cancellable = true)
    private void onGetLightEmission(CallbackInfoReturnable<Integer> cir) {
        if (!Xray.maybeEnabled()) return;
        BlockBehaviour.BlockStateBase self = (BlockBehaviour.BlockStateBase)(Object)this;
        if (Xray.itz().isBlockSelected(self.getBlock())) {

            cir.setReturnValue(15);
        }
    }
}
