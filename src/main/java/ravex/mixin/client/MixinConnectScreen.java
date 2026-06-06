package ravex.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.misc.AutoReconnect;

@Mixin(ConnectScreen.class)
public class MixinConnectScreen {

    @Inject(method = "startConnecting", at = @At("HEAD"))
    private static void onStartConnecting(Screen parent, Minecraft mc, ServerAddress address,
                                          ServerData serverData, boolean p_258689_,
                                          TransferState transferState, CallbackInfo ci) {
        if (serverData != null) {
            AutoReconnect.recordServer(serverData);
        }
    }
}
