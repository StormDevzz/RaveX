package ravex.mixin.client;
import ravex.utility.misc.ScreenUtility;

import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.world.AutoSign;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import ravex.modules.Modules;
@Mixin(AbstractSignEditScreen.class)
public abstract class MixinAbstractSignEditScreen {
    @Shadow @Final private SignBlockEntity sign;
    @Shadow @Final private boolean isFrontText;

    @Inject(method = "init", at = @At("HEAD"))
    private void onInit(CallbackInfo ci) {
        if (Modules.enabled(AutoSign.class)) {
            var mc = MinecraftWrapper.getInstance();
            if (mc.player != null && mc.getConnection() != null && sign != null) {
                AutoSign autoSign = Modules.get(AutoSign.class);
                String l1 = autoSign.line1;
                String l2 = autoSign.line2;
                String l3 = autoSign.line3;
                String l4 = autoSign.line4;

                mc.getConnection().send(new ServerboundSignUpdatePacket(
                    sign.getBlockPos(),
                    isFrontText,
                    l1, l2, l3, l4
                ));

                if (autoSign.isAdvanced()) {
                    autoSign.closeSignScreen();
                } else {
                    mc.execute(() -> ScreenUtility.closeScreen(ravex.mcwrapper.MinecraftWrapper.getWrapper()));
                }
            }
        }
    }
}
