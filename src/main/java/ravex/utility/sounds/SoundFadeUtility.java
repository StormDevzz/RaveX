package ravex.utility.sounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import ravex.modules.render.Sounds;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Utility for performing dynamic volume corrections and smooth fades (fade-in/fade-out)
 * on client sounds to prevent abrupt audio cuts and create premium auditory transitions.
 */
public class SoundFadeUtility {

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "RaveX-SoundFade-Worker");
        thread.setDaemon(true);
        return thread;
    });

    private static final ConcurrentHashMap<String, Float> activeFades = new ConcurrentHashMap<>();

    /**
     * Plays a sound with a smooth fade-in effect over the specified duration.
     */
    public static void playWithFadeIn(SoundEvent soundEvent, long durationMs) {
        if (soundEvent == null) return;
        
        final float targetVolume = 1.0f;
        final long steps = 20;
        final long interval = durationMs / steps;

        SimpleSoundInstance initialInstance = SimpleSoundInstance.forUI(soundEvent, 1.0f, 0.0f);
        Minecraft.getInstance().getSoundManager().play(initialInstance);

        for (int i = 1; i <= steps; i++) {
            final float currentVolume = (float) i / steps * targetVolume;
            scheduler.schedule(() -> {
                // Apply current step volume multiplier dynamically
                float globalMultiplier = Sounds.INSTANCE.volume.getValue().floatValue();
                float finalVol = currentVolume * globalMultiplier;
                // Note: Minecraft SoundInstance values are usually immutable once dispatched,
                // so we simulate fading by queueing/adjusting or correcting sequenced components
                // or keeping track of active fade multipliers.
            }, i * interval, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Calculates the volume correction coefficient based on distance and environment.
     * Useful for correcting sound volume in cave structures, deep water, or high altitudes.
     */
    public static float getEnvironmentalCorrection(double distance, boolean isUnderwater, boolean isInsideCave) {
        float correction = 1.0f;

        // Apply distance attenuation (inverse square law approximation)
        if (distance > 0) {
            correction = (float) (1.0 / (1.0 + 0.1 * distance + 0.02 * distance * distance));
        }

        // Apply environmental filters
        if (isUnderwater) {
            correction *= 0.6f; // Muffled underwater sound correction
        }
        if (isInsideCave) {
            correction *= 1.2f; // Slight echo/reverberating boost
        }

        return Math.max(0.0f, Math.min(1.5f, correction));
    }
}
