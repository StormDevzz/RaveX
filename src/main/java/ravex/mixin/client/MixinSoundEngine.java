package ravex.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
<<<<<<< HEAD
import ravex.modules.misc.SoundBlock;
=======
import ravex.modules.misc.SoundBlocker;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3

@Mixin(SoundEngine.class)
public abstract class MixinSoundEngine {
    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void onPlay(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
<<<<<<< HEAD
        if (SoundBlock.itz().shouldBlock(sound)) {
=======
        if (SoundBlocker.INSTANCE.shouldBlock(sound)) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
        }
    }
}
