package ravex.mixin.world;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.movement.Avoid;

@Mixin(BlockBehaviour.BlockStateBase.class)
public class MixinBlockState {

    @Inject(method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;", at = @At("HEAD"), cancellable = true)
    private void onGetCollisionShape(net.minecraft.world.level.BlockGetter world, net.minecraft.core.BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (Avoid.INSTANCE.getEnabled()) {
            BlockBehaviour.BlockStateBase self = (BlockBehaviour.BlockStateBase)(Object)this;
            if (Avoid.INSTANCE.shouldAvoid(self.getBlock())) {
                cir.setReturnValue(Shapes.block());
            }
        }
    }

    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
    private void onGetRenderShape(CallbackInfoReturnable<net.minecraft.world.level.block.RenderShape> cir) {
        if (ravex.modules.render.NoRender.INSTANCE.getEnabled() && ravex.modules.render.NoRender.INSTANCE.tripwire.getValue()) {
            BlockBehaviour.BlockStateBase self = (BlockBehaviour.BlockStateBase)(Object)this;
            if (self.getBlock() instanceof net.minecraft.world.level.block.TripWireBlock) {
                cir.setReturnValue(net.minecraft.world.level.block.RenderShape.INVISIBLE);
            }
        }
    }
}
