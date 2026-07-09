package ravex.utility.sound;

import ravex.event.Subscribe;
import ravex.event.client.SoundEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import ravex.manager.ModuleManager;

public class SoundEventDispatcher {
    @Subscribe
    public void onSoundEvent(SoundEvent event) {
        net.minecraft.sounds.SoundEvent mcSound = resolveSound(event.getType());
        if (mcSound == null) return;

        ravex.modules.render.Sounds sounds = ModuleManager.get(ravex.modules.render.Sounds.class);
        if (sounds != null && !sounds.getEnabled()) return;

        float multiplier = (sounds != null) ? sounds.volume.getValue().floatValue() : 1.0f;
        float finalVolume = event.getVolume() * multiplier;
        if (finalVolume <= 0.0f) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        ensureMasterVolume(mc);

        final float fv = finalVolume;
        final net.minecraft.sounds.SoundEvent se = mcSound;

        if (mc.isSameThread()) {
            dispatchSound(mc, se, fv);
        } else {
            mc.execute(() -> dispatchSound(mc, se, fv));
        }
    }

    private net.minecraft.sounds.SoundEvent resolveSound(SoundEvent.Type type) {
        return switch (type) {
            case ENABLE -> SoundUtility.ENABLE;
            case DISABLE -> SoundUtility.DISABLE;
            case FAILURE -> SoundUtility.FAILURE;
            case SETTINGS_OPEN -> SoundUtility.SETTINGS_OPEN;
            case SETTINGS_CLOSE -> SoundUtility.SETTINGS_CLOSE;
            case GUI_OPEN -> SoundUtility.GUI_OPEN;
            case GUI_CLOSE -> SoundUtility.GUI_CLOSE;
            default -> null;
        };
    }

    private void ensureMasterVolume(Minecraft mc) {
        try {
            if (mc.options != null) {
                var masterOpt = mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER);
                if (masterOpt != null && masterOpt.get() <= 0.0) {
                    masterOpt.set(1.0);
                    ravex.RaveX.LOGGER.info("[Sound] Master Volume was 0.0, auto-set to 1.0 to enable client sounds.");
                    mc.options.save();
                }
            }
        } catch (Exception e) {
            ravex.RaveX.LOGGER.warn("[Sound] Failed to check/set Master Volume: {}", e.getMessage());
        }
    }

    private void dispatchSound(Minecraft mc, net.minecraft.sounds.SoundEvent soundEvent, float volume) {
        try {
            if (mc.getSoundManager() == null) return;
            if (mc.player != null) {
                mc.player.playSound(soundEvent, volume, 1.0f);
            } else {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(soundEvent, 1.0f, volume));
            }
        } catch (Exception e) {
            ravex.RaveX.LOGGER.warn("[Sound] Playback error: {}", e.getMessage());
        }
    }
}
