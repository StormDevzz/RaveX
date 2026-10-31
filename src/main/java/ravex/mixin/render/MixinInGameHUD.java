package ravex.mixin.render;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.render.NoRender;
import ravex.render.hud.HudRenderer;
import ravex.modules.Modules;

@Mixin(Gui.class)
public abstract class MixinInGameHUD {
    @Shadow private net.minecraft.client.gui.components.PlayerTabOverlay tabList;

    @Inject(method = "renderBossOverlay", at = @At("HEAD"), cancellable = true)
    private void ravexHideBossBar(GuiGraphics g, DeltaTracker d, CallbackInfo ci) {
        if (Modules.enabled(NoRender.class) && Modules.get(NoRender.class).bossBar) ci.cancel();
    }

    @Inject(method = "renderScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void ravexHideScoreboard(GuiGraphics g, DeltaTracker d, CallbackInfo ci) {
        if (Modules.enabled(NoRender.class) && Modules.get(NoRender.class).scoreboard) ci.cancel();
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void ravexHideCrosshair(GuiGraphics g, DeltaTracker d, CallbackInfo ci) {
        if (Modules.enabled(NoRender.class) && Modules.get(NoRender.class).crosshair) ci.cancel();
    }

    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private void ravexHidePotions(GuiGraphics g, DeltaTracker d, CallbackInfo ci) {
        if (Modules.enabled(NoRender.class) && Modules.get(NoRender.class).effectIcons) ci.cancel();
    }

    @Inject(method = "renderVignette", at = @At("HEAD"), cancellable = true)
    private void ravexHideVignette(GuiGraphics g, net.minecraft.world.entity.Entity e, CallbackInfo ci) {
        if (Modules.enabled(NoRender.class) && Modules.get(NoRender.class).vignette) ci.cancel();
    }

    @Inject(method = "renderConfusionOverlay", at = @At("HEAD"), cancellable = true)
    private void ravexHideNausea(GuiGraphics g, float f, CallbackInfo ci) {
        if (Modules.enabled(NoRender.class) && Modules.get(NoRender.class).nausea) ci.cancel();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void ravexTotemOverlay(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        float t = ravex.modules.render.Animations.totemAnimEase();
        if (t >= 1f) return;
        var mc = ravex.mcwrapper.MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null) return;
        ravex.modules.render.Animations m = Modules.get(ravex.modules.render.Animations.class);
        if (m == null) return;
        float cx = context.guiWidth() / 2f;
        float cy = context.guiHeight() / 2f;
        float inv = 1f - t;

        var pose = context.pose();
        pose.pushMatrix();
        switch (m.totemMode) {
            case "Slide" -> {
                float rise = 170f * inv * inv * inv;
                float sc = 3.6f;
                pose.translate(cx, cy + rise);
                pose.scale(sc, sc);
                context.renderItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING), -8, -8);
            }
            case "Zoom" -> {
                float punch = Math.min(1f, t * 1.7f);
                float ease = 1f - (1f - punch) * (1f - punch) * (1f - punch);
                float sc = 0.6f + 7.4f * ease;
                pose.translate(cx, cy);
                pose.scale(sc, sc);
                context.renderItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING), -8, -8);
            }
            default -> {
                float sc = t < 0.3f ? 0.5f + (t / 0.3f) * 5.0f : 5.5f - ((t - 0.3f) / 0.7f) * 2.3f;
                float dip = (float) Math.sin(Math.min(1f, t) * Math.PI) * -26f;
                pose.translate(cx, cy + dip);
                pose.rotate(inv * 1.2f);
                pose.scale(sc, sc);
                context.renderItem(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING), -8, -8);
            }
        }
        pose.popMatrix();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        HudRenderer.INSTANCE.render(context, tickCounter);
        ravexTabFadeOut(context, tickCounter);
        if (ravex.modules.Modules.enabled(ravex.modules.render.LogoutSpots.class)) {
            ravex.modules.render.LogoutSpots.renderLabels(context);
        }
    }

    private void ravexTabFadeOut(GuiGraphics context, DeltaTracker tickCounter) {
        if (!ravex.modules.render.Animations.tabAnimation()) return;
        var mc = ravex.mcwrapper.MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getOptions() == null) return;
        if (mc.getOptions().keyPlayerList.isDown()) return;
        float fade = ravex.modules.render.Animations.tabFade;
        if (fade <= 0.001f) {
            ravex.modules.render.Animations.tabLastNs = 0L;
            return;
        }
        long now = System.nanoTime();
        float dt = 0.05f;
        if (ravex.modules.render.Animations.tabLastNs != 0L) {
            dt = Math.min(0.1f, (now - ravex.modules.render.Animations.tabLastNs) / 1_000_000_000f);
        }
        ravex.modules.render.Animations.tabLastNs = now;
        fade = Math.max(0f, fade - dt * 7f);
        ravex.modules.render.Animations.tabFade = fade;
        if (fade <= 0.001f) return;
        float e = 1f - (1f - fade) * (1f - fade) * (1f - fade);
        float scale = 0.94f + 0.06f * e;
        var pose = context.pose();
        float w = context.guiWidth() / 2f;
        pose.pushMatrix();
        pose.translate(w, 8f);
        pose.scale(scale, scale);
        pose.translate(-w, -8f);
        pose.translate(0f, -10f * (1f - e));
        tabList.setVisible(true);
        tabList.render(context, context.guiWidth(),
            mc.getLevel() != null ? mc.getLevel().getScoreboard() : null, null);
        tabList.setVisible(false);
        pose.popMatrix();
    }

    @Inject(method = "renderItemHotbar", at = @At("HEAD"), cancellable = true)
    private void ravexHideVanillaHotbar(GuiGraphics g, DeltaTracker d, CallbackInfo ci) {
        if (ravex.modules.hud.HotbarHud.hidesVanillaHotbar()) ci.cancel();
    }

    @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
    private void onRenderPortalOverlay(GuiGraphics guiGraphics, float f, CallbackInfo ci) {
        if (Modules.enabled(NoRender.class) && Modules.get(NoRender.class).portal) {
            ci.cancel();
        }
    }
}