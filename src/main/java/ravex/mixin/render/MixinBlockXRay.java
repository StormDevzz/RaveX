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

    // ─── Make non-selected blocks invisible ───────────────────────────────────

    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
    private void onGetRenderShape(CallbackInfoReturnable<RenderShape> cir) {
        if (!Xray.INSTANCE.getEnabled()) return;
        BlockBehaviour.BlockStateBase self = (BlockBehaviour.BlockStateBase)(Object)this;
        if (!Xray.INSTANCE.isBlockSelected(self.getBlock())) {
            cir.setReturnValue(RenderShape.INVISIBLE);
        }
    }

    // ─── Face culling: selected blocks always show all faces ──────────────────

    @Inject(method = "skipRendering", at = @At("HEAD"), cancellable = true)
    private void onSkipRendering(BlockState adjacent, Direction dir, CallbackInfoReturnable<Boolean> cir) {
        if (!Xray.INSTANCE.getEnabled()) return;
        BlockBehaviour.BlockStateBase self = (BlockBehaviour.BlockStateBase)(Object)this;
        if (Xray.INSTANCE.isBlockSelected(self.getBlock())) {
            // Selected block: never skip any face (render all 6 sides)
            cir.setReturnValue(false);
        } else {
            // Non-selected block: always skip (we're invisible, let neighbors be seen)
            cir.setReturnValue(true);
        }
    }

    // ─── Occlusion shape: non-selected blocks have no occlusion ──────────────
    // This is THE key fix. Minecraft uses these shapes during chunk building
    // to decide whether to include a face in the mesh. If the adjacent block
    // has a full occlusion shape, the face of the current block is NEVER added
    // to the chunk mesh — regardless of what skipRendering says.
    // By returning empty shapes for non-selected blocks, their neighbors' faces
    // are always baked into the mesh and become visible through xray.

    @Inject(method = "getOcclusionShape", at = @At("HEAD"), cancellable = true)
    private void onGetOcclusionShape(CallbackInfoReturnable<VoxelShape> cir) {
        if (!Xray.INSTANCE.getEnabled()) return;
        BlockBehaviour.BlockStateBase self = (BlockBehaviour.BlockStateBase)(Object)this;
        if (!Xray.INSTANCE.isBlockSelected(self.getBlock())) {
            cir.setReturnValue(Shapes.empty());
        }
    }

    @Inject(method = "getFaceOcclusionShape", at = @At("HEAD"), cancellable = true)
    private void onGetFaceOcclusionShape(Direction direction, CallbackInfoReturnable<VoxelShape> cir) {
        if (!Xray.INSTANCE.getEnabled()) return;
        BlockBehaviour.BlockStateBase self = (BlockBehaviour.BlockStateBase)(Object)this;
        if (!Xray.INSTANCE.isBlockSelected(self.getBlock())) {
            cir.setReturnValue(Shapes.empty());
        }
    }

    // ─── isSolidRender: prevent face culling at render time ───────────────────

    @Inject(method = "isSolidRender", at = @At("HEAD"), cancellable = true)
    private void onIsSolidRender(CallbackInfoReturnable<Boolean> cir) {
        if (!Xray.INSTANCE.getEnabled()) return;
        BlockBehaviour.BlockStateBase self = (BlockBehaviour.BlockStateBase)(Object)this;
        if (!Xray.INSTANCE.isBlockSelected(self.getBlock())) {
            cir.setReturnValue(false);
        }
    }

    // ─── Light: let light pass through, selected blocks glow ─────────────────

    @Inject(method = "getLightBlock", at = @At("HEAD"), cancellable = true)
    private void onGetLightBlock(CallbackInfoReturnable<Integer> cir) {
        if (Xray.INSTANCE.getEnabled()) {
            cir.setReturnValue(0);
        }
    }

    @Inject(method = "getLightEmission", at = @At("HEAD"), cancellable = true)
    private void onGetLightEmission(CallbackInfoReturnable<Integer> cir) {
        if (!Xray.INSTANCE.getEnabled()) return;
        BlockBehaviour.BlockStateBase self = (BlockBehaviour.BlockStateBase)(Object)this;
        if (Xray.INSTANCE.isBlockSelected(self.getBlock())) {
            // Selected blocks glow like glowstone (max light level 15)
            cir.setReturnValue(15);
        }
    }
}
