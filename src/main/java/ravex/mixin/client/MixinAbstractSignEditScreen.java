package ravex.mixin.client;
import ravex.utility.misc.ScreenUtility;

import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.world.AutoSign;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
@Mixin(AbstractSignEditScreen.class)
public abstract class MixinAbstractSignEditScreen {
    @Shadow @Final private SignBlockEntity sign;
    @Shadow @Final private boolean isFrontText;

    @Inject(method = "init", at = @At("HEAD"))
    private void onInit(CallbackInfo ci) {
        if (AutoSign.maybeEnabled()) {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null && mc.getConnection() != null && sign != null) {
                String l1 = AutoSign.itz().line1;
                String l2 = AutoSign.itz().line2;
                String l3 = AutoSign.itz().line3;
                String l4 = AutoSign.itz().line4;

                mc.getConnection().send(new ServerboundSignUpdatePacket(
                    sign.getBlockPos(),
                    isFrontText,
                    l1, l2, l3, l4
                ));


                mc.execute(() -> ScreenUtility.closeScreen(ravex.mcwrapper.MinecraftWrapper.getWrapper()));
            }
        }
    }
}
