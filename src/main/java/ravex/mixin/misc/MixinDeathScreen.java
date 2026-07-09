package ravex.mixin.misc;

import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
<<<<<<< HEAD
import ravex.modules.render.DeathText;
=======
import ravex.modules.misc.CustomDeathText;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3

@Mixin(DeathScreen.class)
public abstract class MixinDeathScreen {

    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, index = 1)
    private static Component modifyDeathMessage(Component original) {
<<<<<<< HEAD
        Component custom = DeathText.getDeathComponent();
=======
        Component custom = CustomDeathText.getDeathComponent();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (custom != null) {
            return custom;
        }
        return original;
    }
}
