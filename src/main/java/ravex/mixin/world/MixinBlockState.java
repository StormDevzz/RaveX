package ravex.mixin.world;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.movement.Avoid;
import ravex.modules.movement.LiquidControl;
import ravex.modules.movement.Phase;
import ravex.modules.render.NoRender;
import ravex.modules.world.GhostBlocks;

@Mixin(BlockBehaviour.BlockStateBase.class)
public class MixinBlockState {

    @Inject(method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;", at = @At("HEAD"), cancellable = true)
    private void onGetCollisionShape(net.minecraft.world.level.BlockGetter world, net.minecraft.core.BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
<<<<<<< HEAD
        if (LiquidControl.maybeEnabled()) {
            if (context instanceof net.minecraft.world.phys.shapes.EntityCollisionContext ecc) {
                if (ecc.getEntity() != null && ecc.getEntity() == net.minecraft.client.Minecraft.getInstance().player) {
                    BlockBehaviour.BlockStateBase self = (BlockBehaviour.BlockStateBase)(Object)this;
                    boolean bypassWater = LiquidControl.itz().water.getValue();
                    boolean bypassLava = LiquidControl.itz().lava.getValue();
                    boolean bypassOthers = LiquidControl.itz().others.getValue();
=======
        if (ravex.modules.movement.LiquidCollision.INSTANCE.getEnabled()) {
            if (context instanceof net.minecraft.world.phys.shapes.EntityCollisionContext ecc) {
                if (ecc.getEntity() != null && ecc.getEntity() == net.minecraft.client.Minecraft.getInstance().player) {
                    BlockBehaviour.BlockStateBase self = (BlockBehaviour.BlockStateBase)(Object)this;
                    boolean bypassWater = ravex.modules.movement.LiquidCollision.INSTANCE.water.getValue();
                    boolean bypassLava = ravex.modules.movement.LiquidCollision.INSTANCE.lava.getValue();
                    boolean bypassOthers = ravex.modules.movement.LiquidCollision.INSTANCE.others.getValue();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                    
                    net.minecraft.world.level.material.FluidState fluid = self.getFluidState();
                    if (!fluid.isEmpty()) {
                        if (fluid.is(net.minecraft.tags.FluidTags.WATER) && bypassWater) {
                            cir.setReturnValue(Shapes.empty());
                            return;
                        }
                        if (fluid.is(net.minecraft.tags.FluidTags.LAVA) && bypassLava) {
                            cir.setReturnValue(Shapes.empty());
                            return;
                        }
                        if (bypassOthers) {
                            cir.setReturnValue(Shapes.empty());
                            return;
                        }
                    }
                }
            }
        }

<<<<<<< HEAD
        if (Phase.maybeEnabled()) {
=======
        if (ravex.modules.movement.Phase.INSTANCE.getEnabled()) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            if (context instanceof net.minecraft.world.phys.shapes.EntityCollisionContext ecc) {
                if (ecc.getEntity() != null && ecc.getEntity() instanceof net.minecraft.client.player.LocalPlayer) {
                    cir.setReturnValue(Shapes.empty());
                    return;
                }
            }
        }

<<<<<<< HEAD
        if (Avoid.maybeEnabled()) {
=======
        if (Avoid.INSTANCE.getEnabled()) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            BlockBehaviour.BlockStateBase self = (BlockBehaviour.BlockStateBase)(Object)this;
            if (Avoid.itz().shouldAvoid(self.getBlock())) {
                cir.setReturnValue(Shapes.block());
            }
        }
    }

    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
    private void onGetRenderShape(CallbackInfoReturnable<net.minecraft.world.level.block.RenderShape> cir) {
        if (NoRender.maybeEnabled() && NoRender.itz().tripwire.getValue()) {
            BlockBehaviour.BlockStateBase self = (BlockBehaviour.BlockStateBase)(Object)this;
            if (self.getBlock() instanceof net.minecraft.world.level.block.TripWireBlock) {
                cir.setReturnValue(net.minecraft.world.level.block.RenderShape.INVISIBLE);
            }
        }
    }

    @Inject(method = "getVisualShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("HEAD"), cancellable = true)
    private void onGetVisualShape(net.minecraft.world.level.BlockGetter world, net.minecraft.core.BlockPos pos,
                                  net.minecraft.world.phys.shapes.CollisionContext context,
                                  CallbackInfoReturnable<VoxelShape> cir) {
<<<<<<< HEAD
        if (GhostBlocks.maybeEnabled()) {
            net.minecraft.world.level.block.state.BlockState self =
                (net.minecraft.world.level.block.state.BlockState)(Object)this;
            String blockId = ravex.modules.world.GhostBlocks.getBlockId(self);
            if (ravex.modules.world.GhostBlocks.isGhostBlock(pos.getX(), pos.getY(), pos.getZ(), blockId)) {
=======
        if (ravex.modules.world.NoGhostBlocks.INSTANCE.getEnabled()) {
            net.minecraft.world.level.block.state.BlockState self =
                (net.minecraft.world.level.block.state.BlockState)(Object)this;
            String blockId = ravex.modules.world.NoGhostBlocks.getBlockId(self);
            if (ravex.modules.world.NoGhostBlocks.isGhostBlock(pos.getX(), pos.getY(), pos.getZ(), blockId)) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                cir.setReturnValue(Shapes.empty());
            }
        }
    }
}

