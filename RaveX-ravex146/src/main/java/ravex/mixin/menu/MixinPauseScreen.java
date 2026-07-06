package ravex.mixin.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.misc.AutoReconnect;

@Mixin(PauseScreen.class)
public abstract class MixinPauseScreen extends Screen {

    protected MixinPauseScreen(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        if (!AutoReconnect.INSTANCE.getEnabled()) return;
        if (!AutoReconnect.hasLastServer()) return;

        int btnW = 200;
        int btnX = this.width / 2 - btnW / 2;
        int btnY = this.height / 4 + 144;

        this.addRenderableWidget(Button.builder(
            Component.literal("Reconnect"),
            btn -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.getConnection() != null) {
                    mc.getConnection().getConnection().disconnect(Component.literal("Reconnecting"));
                }
                mc.setScreen(null);
                AutoReconnect.reconnect(mc);
            }
        ).bounds(btnX, btnY, btnW, 20).build());
    }
}
