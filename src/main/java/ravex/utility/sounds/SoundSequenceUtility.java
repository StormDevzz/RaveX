package ravex.utility.sounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import ravex.modules.render.Sounds;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Utility for arranging, sequencing, and dispatching sound sequences, alerts,
 * and multi-tonal notifications.
 */
public class SoundSequenceUtility {

    private static final ScheduledExecutorService sequencer = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "RaveX-SoundSequence-Worker");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * Plays a pleasant ascending pentatonic arpeggio sequence using a single SoundEvent (e.g. settings_open).
     */
    public static void playAscendingArpeggio(SoundEvent soundEvent, long stepDelayMs) {
        if (soundEvent == null) return;

        float[] pitches = { 1.0f, 1.25f, 1.5f, 2.0f };

        for (int i = 0; i < pitches.length; i++) {
            final float pitch = pitches[i];
            sequencer.schedule(() -> playCustomSound(soundEvent, pitch, 0.8f), i * stepDelayMs, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Plays a two-tone success notification chime (high to higher pitch).
     */
    public static void playSuccessChime(SoundEvent soundEvent) {
        if (soundEvent == null) return;
        playCustomSound(soundEvent, 1.2f, 0.9f);
        sequencer.schedule(() -> playCustomSound(soundEvent, 1.6f, 0.9f), 120, TimeUnit.MILLISECONDS);
    }

    /**
     * Plays a warning double-chime (low pitch to lower pitch).
     */
    public static void playWarningChime(SoundEvent soundEvent) {
        if (soundEvent == null) return;
        playCustomSound(soundEvent, 0.8f, 1.0f);
        sequencer.schedule(() -> playCustomSound(soundEvent, 0.6f, 1.0f), 150, TimeUnit.MILLISECONDS);
    }

    /**
     * Helper to play a custom sound event with specific pitch and volume settings immediately.
     */
    private static void playCustomSound(SoundEvent soundEvent, float pitch, float baseVolume) {
        try {
            // Apply client sound module volumes
            float multiplier = 1.0f;
            if (Sounds.INSTANCE != null) {
                if (!Sounds.INSTANCE.getEnabled()) return;
                multiplier = Sounds.INSTANCE.volume.getValue().floatValue();
            }
            float finalVolume = baseVolume * multiplier;
            if (finalVolume <= 0.0f) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getSoundManager() != null) {
                // simple sounds must run on main/render thread or through the thread-safe SoundManager
                mc.execute(() -> {
                    SimpleSoundInstance sound = SimpleSoundInstance.forUI(soundEvent, pitch, finalVolume);
                    mc.getSoundManager().play(sound);
                });
            }
        } catch (Exception ignored) {}
    }
}
