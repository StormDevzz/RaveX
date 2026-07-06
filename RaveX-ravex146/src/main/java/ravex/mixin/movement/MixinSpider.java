package ravex.mixin.movement;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.movement.Spider;

@Mixin(LocalPlayer.class)
public class MixinSpider {
    @Inject(method = "aiStep", at = @At("TAIL"))
    private void onAiStep(CallbackInfo ci) {
        if (!Spider.INSTANCE.getEnabled()) return;

        LocalPlayer player = (LocalPlayer)(Object)this;
        if (!player.input.keyPresses.jump()) return;

        if (!player.horizontalCollision && !isAgainstWall(player)) return;

        String mode = Spider.INSTANCE.mode.getValue();
        double motion;

        switch (mode) {
            case "Normal" -> motion = 0.2;
            case "NCP" -> motion = 0.18;
            case "Custom" -> motion = Spider.INSTANCE.motion.getValue();
            default -> motion = 0.2;
        }

        Vec3 vel = player.getDeltaMovement();
        player.setDeltaMovement(vel.x, motion, vel.z);
        player.setOnGround(true);
    }

    private static boolean isAgainstWall(LocalPlayer player) {
        Vec3 eye = player.getEyePosition();
        int bx = (int) Math.floor(eye.x);
        int by = (int) Math.floor(eye.y);
        int bz = (int) Math.floor(eye.z);
        return !player.level().getBlockState(new BlockPos(bx + 1, by, bz)).isAir()
            || !player.level().getBlockState(new BlockPos(bx - 1, by, bz)).isAir()
            || !player.level().getBlockState(new BlockPos(bx, by, bz + 1)).isAir()
            || !player.level().getBlockState(new BlockPos(bx, by, bz - 1)).isAir()
            || !player.level().getBlockState(new BlockPos(bx + 1, by + 1, bz)).isAir()
            || !player.level().getBlockState(new BlockPos(bx - 1, by + 1, bz)).isAir()
            || !player.level().getBlockState(new BlockPos(bx, by + 1, bz + 1)).isAir()
            || !player.level().getBlockState(new BlockPos(bx, by + 1, bz - 1)).isAir();
    }
}
