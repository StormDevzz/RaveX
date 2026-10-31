package ravex.mixin.player;

import net.minecraft.client.gui.GuiGraphics;
import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.player.TabHelper;
import ravex.modules.misc.NameProtect;

import java.util.List;
import ravex.modules.Modules;

@Mixin(PlayerTabOverlay.class)
public class MixinPlayerTabOverlay {

    private boolean ravexTabPushed;

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(net.minecraft.client.gui.GuiGraphics graphics, int listSize,
            net.minecraft.world.scores.Scoreboard scoreboard, net.minecraft.world.scores.Objective objective,
            CallbackInfo ci) {
        if (!ravex.modules.render.Animations.tabAnimation()) {
            ravexTabPushed = false;
            return;
        }
        long now = System.nanoTime();
        float dt = 0f;
        if (ravex.modules.render.Animations.tabLastNs != 0L) {
            dt = Math.min(0.1f, (now - ravex.modules.render.Animations.tabLastNs) / 1_000_000_000f);
        }
        ravex.modules.render.Animations.tabLastNs = now;
        // ease the list in from the top
        float fade = Math.min(1f, ravex.modules.render.Animations.tabFade + dt * 8f);
        ravex.modules.render.Animations.tabFade = fade;
        float e = 1f - (1f - fade) * (1f - fade) * (1f - fade);
        float scale = 0.94f + 0.06f * e;
        float w = graphics.guiWidth() / 2f;
        graphics.pose().pushMatrix();
        graphics.pose().translate(w, 8f);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-w, -8f);
        graphics.pose().translate(0f, -10f * (1f - e));
        ravexTabPushed = true;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderTail(net.minecraft.client.gui.GuiGraphics graphics, int listSize,
            net.minecraft.world.scores.Scoreboard scoreboard, net.minecraft.world.scores.Objective objective,
            CallbackInfo ci) {
        if (ravexTabPushed) {
            ravexTabPushed = false;
            graphics.pose().popMatrix();
        }
    }

    @Inject(method = "setVisible", at = @At("HEAD"))
    private void onSetVisible(boolean visible, CallbackInfo ci) {
        if (!visible && ravex.modules.render.Animations.tabAnimation()) {
            // let the hud mixin play the closing slide
            ravex.modules.render.Animations.tabLastNs = 0L;
        }
    }

    @Inject(method = "getPlayerInfos", at = @At("RETURN"), cancellable = true)
    private void onGetPlayerInfos(CallbackInfoReturnable<List<PlayerInfo>> cir) {
        if (Modules.enabled(TabHelper.class)) {
            List<PlayerInfo> list = cir.getReturnValue();
            int limit = (int) Modules.get(TabHelper.class).limit;
            if (list.size() > limit) {
                cir.setReturnValue(list.subList(0, limit));
            }
        }
    }

    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void onGetNameForDisplay(PlayerInfo playerInfo, CallbackInfoReturnable<Component> cir) {
        // protect the name before colors are applied
        if (Modules.enabled(NameProtect.class)) {
            Component protectedName = Modules.get(NameProtect.class).protectComponent(cir.getReturnValue());
            cir.setReturnValue(protectedName);
        }
        if (Modules.enabled(TabHelper.class)) {
            Component name = cir.getReturnValue();
            String rawName = playerInfo.getProfile().name();
            String nameStr = name != null ? name.getString() : rawName;

            var mc = MinecraftWrapper.getInstance();
            if (rawName.equals(mc.getUser().getName())) {
                int selfColor = Modules.get(TabHelper.class).selfColor;
                cir.setReturnValue(Component.literal(nameStr).withStyle(style -> style.withColor(selfColor)));
            } else if (ravex.manager.FriendManager.INSTANCE.isFriend(rawName)) {
                int friendColor = Modules.get(TabHelper.class).friendColor;
                cir.setReturnValue(Component.literal(nameStr).withStyle(style -> style.withColor(friendColor)));
            }
        }
    }

    @Inject(method = "renderPingIcon", at = @At("HEAD"), cancellable = true)
    private void onRenderPingIcon(GuiGraphics graphics, int width, int x, int y, PlayerInfo playerInfo, CallbackInfo ci) {
        if (Modules.enabled(TabHelper.class) && Modules.get(TabHelper.class).showPing) {
            ci.cancel();
            int latency = playerInfo.getLatency();
            String pingStr = latency + "ms";

            int color = 0xFF55FF55;
            if (latency > 150) {
                color = 0xFFFF5555;
            } else if (latency > 85) {
                color = 0xFFFFCC33;
            }

            // clear of the vanilla ping suffix
            graphics.drawString(MinecraftWrapper.getInstance().font, pingStr, x + width + 4, y + 1, color, false);
        }
    }
}
