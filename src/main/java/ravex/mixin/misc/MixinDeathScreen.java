package ravex.mixin.misc;

import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ravex.modules.render.DeathText;

@Mixin(DeathScreen.class)
public abstract class MixinDeathScreen {

    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, index = 1)
    private static Component modifyDeathMessage(Component original) {
        Component custom = DeathText.getDeathComponent();
        if (custom != null) {
            return custom;
        }
        return original;
    }
}
