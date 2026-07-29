package ravex.mixin.menu;

import net.minecraft.client.gui.components.Button;
import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.misc.AutoReconnect;
import ravex.modules.Modules;

@Mixin(DisconnectedScreen.class)
public abstract class MixinDisconnectedScreen extends Screen {

    protected MixinDisconnectedScreen(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        if (!Modules.enabled(AutoReconnect.class)) return;
        if (!AutoReconnect.hasLastServer()) return;

        int btnW = 200;
        int btnX = this.width / 2 - btnW / 2;
        int btnY = this.height / 2 + 36;

        this.addRenderableWidget(Button.builder(
            Component.literal("Reconnect"),
            btn -> {
                var mc = MinecraftWrapper.getInstance();
                AutoReconnect.reconnect(ravex.mcwrapper.MinecraftWrapper.getWrapper());
            }
        ).bounds(btnX, btnY, btnW, 20).build());

        Modules.get(AutoReconnect.class).scheduleAutoReconnect();
    }
}
