package ravex.mixin.player;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.player.ExtraTab;

import java.util.List;

@Mixin(PlayerTabOverlay.class)
public class MixinPlayerTabOverlay {


    @Inject(method = "getPlayerInfos", at = @At("RETURN"), cancellable = true)
    private void onGetPlayerInfos(CallbackInfoReturnable<List<PlayerInfo>> cir) {
        if (ExtraTab.INSTANCE.getEnabled()) {
            List<PlayerInfo> list = cir.getReturnValue();
            int limit = ExtraTab.INSTANCE.limit.getValue().intValue();
            if (list.size() > limit) {
                cir.setReturnValue(list.subList(0, limit));
            }
        }
    }

    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void onGetNameForDisplay(PlayerInfo playerInfo, CallbackInfoReturnable<Component> cir) {
        if (ExtraTab.INSTANCE.getEnabled()) {
            Component name = cir.getReturnValue();
            String rawName = playerInfo.getProfile().name();
            String nameStr = name != null ? name.getString() : rawName;

            var mc = net.minecraft.client.Minecraft.getInstance();
            if (rawName.equals(mc.getUser().getName())) {
                int selfColor = ExtraTab.INSTANCE.selfColor.getValue();
                cir.setReturnValue(Component.literal(nameStr).withStyle(style -> style.withColor(selfColor)));
            } else if (ravex.manager.FriendManager.INSTANCE.isFriend(rawName)) {
                int friendColor = ExtraTab.INSTANCE.friendColor.getValue();
                cir.setReturnValue(Component.literal(nameStr).withStyle(style -> style.withColor(friendColor)));
            }
        }
    }

    @Inject(method = "renderPingIcon", at = @At("HEAD"), cancellable = true)
    private void onRenderPingIcon(GuiGraphics graphics, int width, int x, int y, PlayerInfo playerInfo, CallbackInfo ci) {
        if (ExtraTab.INSTANCE.getEnabled() && ExtraTab.INSTANCE.showPing.getValue()) {
            ci.cancel();
            int latency = playerInfo.getLatency();
            String pingStr = latency + "ms";

            int color = 0xFF55FF55;
            if (latency > 150) {
                color = 0xFFFF5555;
            } else if (latency > 85) {
                color = 0xFFFFCC33;
            }

            int textW = net.minecraft.client.Minecraft.getInstance().font.width(pingStr);
            graphics.drawString(net.minecraft.client.Minecraft.getInstance().font, pingStr, x + width - textW - 2, y + 1, color, false);
        }
    }
}
