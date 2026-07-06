package ravex.mixin.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.block.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.player.NoInteract;

@Mixin(Minecraft.class)
public class MixinNoInteract {

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void onStartUseItem(CallbackInfo ci) {
        if (!NoInteract.INSTANCE.getEnabled()) return;

        Minecraft mc = (Minecraft)(Object)this;
        LocalPlayer player = mc.player;
        if (player == null) return;

        if (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = mc.level.getBlockState(pos);
            Block block = state.getBlock();

            if (NoInteract.INSTANCE.containers.getValue() && isContainer(block)) {
                ci.cancel();
            }
            if (NoInteract.INSTANCE.craftingTables.getValue() && block instanceof CraftingTableBlock) {
                ci.cancel();
            }
            if (NoInteract.INSTANCE.buttons.getValue() && isButtonOrLever(block)) {
                ci.cancel();
            }
        }
    }

    private static boolean isContainer(Block block) {
        return block instanceof ChestBlock
            || block instanceof EnderChestBlock
            || block instanceof BarrelBlock
            || block instanceof ShulkerBoxBlock
            || block instanceof HopperBlock
            || block instanceof DispenserBlock
            || block instanceof DropperBlock
            || block instanceof FurnaceBlock
            || block instanceof AbstractFurnaceBlock
            || block instanceof BrewingStandBlock
            || block instanceof BeaconBlock
            || block instanceof EnchantingTableBlock
            || block instanceof AnvilBlock
            || block instanceof GrindstoneBlock
            || block instanceof StonecutterBlock
            || block instanceof LoomBlock
            || block instanceof CartographyTableBlock
            || block instanceof SmithingTableBlock;
    }

    private static boolean isButtonOrLever(Block block) {
        return block instanceof ButtonBlock || block instanceof LeverBlock;
    }
}
