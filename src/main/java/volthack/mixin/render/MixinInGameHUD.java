package volthack.mixin.render;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import volthack.hud.HUDManager;

@Mixin(Gui.class)
public abstract class MixinInGameHUD {
    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (volthack.modules.render.Ambience.INSTANCE.getEnabled()) {
            volthack.modules.render.Ambience.INSTANCE.renderScreen(context);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        HUDManager.INSTANCE.render(context);
        volthack.util.render.NotificationManager.INSTANCE.render(context);
        volthack.modules.render.ESP.INSTANCE.render2D(context);
        volthack.modules.render.NameTags.INSTANCE.render2D(context);
        volthack.modules.render.Spawner.INSTANCE.render2D(context);
    }

    @Inject(method = "renderHotbarAndDecorations(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V", at = @At("HEAD"), cancellable = true)
    private void onRenderHotbar(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        volthack.hud.HUDElement hotbar = HUDManager.INSTANCE.get("Hotbar");
        if (hotbar != null && hotbar.getEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderCrosshair(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V", at = @At("HEAD"), cancellable = true)
    private void onRenderCrosshair(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        volthack.hud.HUDElement crosshair = HUDManager.INSTANCE.get("Crosshair");
        if (crosshair != null && crosshair.getEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void onRenderScoreboard(GuiGraphics guiGraphics, net.minecraft.world.scores.Objective objective, CallbackInfo ci) {
        if (volthack.modules.render.NoRender.INSTANCE.getEnabled() && volthack.modules.render.NoRender.INSTANCE.getScoreboard()) {
            ci.cancel();
        }
    }
}
