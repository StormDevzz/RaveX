package ravex.mixin.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.gui.proxy.ProxyConfigScreen;

@Mixin(JoinMultiplayerScreen.class)
public abstract class MixinMultiplayerScreen extends Screen {

    protected MixinMultiplayerScreen(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        this.addRenderableWidget(Button.builder(
            Component.literal("Proxy Config"),
            btn -> Minecraft.getInstance().setScreen(new ProxyConfigScreen((JoinMultiplayerScreen)(Object)this))
        ).bounds(this.width - 110, this.height - 30, 100, 20).build());
    }
}
