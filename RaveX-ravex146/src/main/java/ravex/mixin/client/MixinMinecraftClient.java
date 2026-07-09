package ravex.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.RaveX;

@Mixin(Minecraft.class)
public abstract class MixinMinecraftClient {
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        RaveX.onClientTick();
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        if (screen instanceof BookViewScreen || screen instanceof BookEditScreen) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                ItemStack main = mc.player.getMainHandItem();
                ItemStack off = mc.player.getOffhandItem();
                if (ravex.modules.misc.AntiBookBan.INSTANCE.shouldBlock(main)
                        || ravex.modules.misc.AntiBookBan.INSTANCE.shouldBlock(off)) {
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "disconnectFromWorld", at = @At("HEAD"), cancellable = true)
    private void onDisconnectFromWorld(net.minecraft.network.chat.Component component, CallbackInfo ci) {
        if (ravex.modules.misc.AntiQuit.INSTANCE.shouldBlockDisconnect()) {
            ci.cancel();
        }
    }

    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void onShouldEntityAppearGlowing(net.minecraft.world.entity.Entity entity, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (ravex.modules.render.ESP.INSTANCE.getEnabled() && ravex.modules.render.ESP.INSTANCE.mode.getValue().equals("Outline")) {
            var mc = Minecraft.getInstance();
            if (entity == mc.player) return;

            if (mc.player != null && mc.player.distanceTo(entity) > ravex.modules.render.ESP.INSTANCE.maxDistance.getValue()) {
                return;
            }

            if (entity instanceof net.minecraft.world.entity.player.Player) {
                if (ravex.modules.render.ESP.INSTANCE.players.getValue()) {
                    cir.setReturnValue(true);
                }
            } else if (entity instanceof net.minecraft.world.entity.monster.Monster) {
                if (ravex.modules.render.ESP.INSTANCE.monsters.getValue()) {
                    cir.setReturnValue(true);
                }
            } else if (entity instanceof net.minecraft.world.entity.animal.Animal || entity instanceof net.minecraft.world.entity.ambient.AmbientCreature) {
                if (ravex.modules.render.ESP.INSTANCE.animals.getValue()) {
                    cir.setReturnValue(true);
                }
            } else if (entity instanceof net.minecraft.world.entity.item.ItemEntity) {
                if (ravex.modules.render.ESP.INSTANCE.items.getValue()) {
                    cir.setReturnValue(true);
                }
            } else if (entity instanceof net.minecraft.world.entity.decoration.ItemFrame) {
                if (ravex.modules.render.ESP.INSTANCE.frames.getValue()) {
                    cir.setReturnValue(true);
                }
            }
        }
    }
}
