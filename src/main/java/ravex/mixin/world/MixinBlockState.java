package ravex.mixin.world;

import net.minecraft.world.level.block.state.BlockBehaviour;
import ravex.mcwrapper.MinecraftWrapper;
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
import ravex.modules.Modules;

@Mixin(BlockBehaviour.BlockStateBase.class)
public class MixinBlockState {

    @Inject(method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;", at = @At("HEAD"), cancellable = true)
    private void onGetCollisionShape(net.minecraft.world.level.BlockGetter world, net.minecraft.core.BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context, CallbackInfoReturnable<VoxelShape> cir) {
        if (Modules.enabled(LiquidControl.class)) {
            if (context instanceof net.minecraft.world.phys.shapes.EntityCollisionContext ecc) {
                if (ecc.getEntity() != null && ecc.getEntity() == MinecraftWrapper.getInstance().player) {
                    BlockBehaviour.BlockStateBase self = (BlockBehaviour.BlockStateBase)(Object)this;
                    boolean bypassWater = Modules.get(LiquidControl.class).water;
                    boolean bypassLava = Modules.get(LiquidControl.class).lava;
                    boolean bypassOthers = Modules.get(LiquidControl.class).others;

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

        if (Modules.enabled(Phase.class)) {
            if (context instanceof net.minecraft.world.phys.shapes.EntityCollisionContext ecc) {
                if (ecc.getEntity() != null && ecc.getEntity() instanceof net.minecraft.client.player.LocalPlayer) {
                    cir.setReturnValue(Shapes.empty());
                    return;
                }
            }
        }

        if (Modules.enabled(Avoid.class)) {
            BlockBehaviour.BlockStateBase self = (BlockBehaviour.BlockStateBase)(Object)this;
            if (Modules.get(Avoid.class).shouldAvoid(self.getBlock())) {
                cir.setReturnValue(Shapes.block());
            }
        }
    }

    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
    private void onGetRenderShape(CallbackInfoReturnable<net.minecraft.world.level.block.RenderShape> cir) {
        if (Modules.enabled(NoRender.class) && Modules.get(NoRender.class).tripwire) {
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
        if (Modules.enabled(GhostBlocks.class)) {
            net.minecraft.world.level.block.state.BlockState self =
                (net.minecraft.world.level.block.state.BlockState)(Object)this;
            String blockId = ravex.modules.world.GhostBlocks.getBlockId(self);
            if (ravex.modules.world.GhostBlocks.isGhostBlock(pos.getX(), pos.getY(), pos.getZ(), blockId)) {
                cir.setReturnValue(Shapes.empty());
            }
        }
    }
}

