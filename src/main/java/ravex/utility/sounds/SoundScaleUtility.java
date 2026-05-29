package ravex.utility.sounds;

/**
 * Utility for pitch correction, harmonization, and kinematic auditory corrections (Doppler effect).
 * Ensures custom client sounds feel acoustically pleasing and harmonize with the gameplay pace.
 */
public class SoundScaleUtility {

    // A minor pentatonic scale multipliers for perfect client harmonies (C, D, E, G, A)
    private static final float[] PENTATONIC_SCALE = {
        1.0f,       // Root note (pitch = 1.0)
        1.122f,     // Major Second
        1.259f,     // Major Third
        1.498f,     // Perfect Fifth
        1.681f,     // Major Sixth
        2.0f        // Octave
    };

    /**
     * Snap any raw pitch to the nearest pentatonic scale step.
     * Prevents dissonant custom overlay notes and creates beautiful chimes.
     */
    public static float harmonizePitch(float rawPitch) {
        float closest = PENTATONIC_SCALE[0];
        float minDiff = Math.abs(rawPitch - closest);

        for (float step : PENTATONIC_SCALE) {
            float diff = Math.abs(rawPitch - step);
            if (diff < minDiff) {
                minDiff = diff;
                closest = step;
            }
        }
        return closest;
    }

    /**
     * Calculates the Doppler pitch shift for a moving sound source relative to the player.
     * @param sourceVelocity m/s of the emitting entity or block
     * @param listenerVelocity m/s of the player
     * @param distanceVector relative position vector
     * @return corrected pitch multiplier
     */
    public static float calculateDopplerShift(double sourceVelocity, double listenerVelocity, double[] distanceVector) {
        double speedOfSound = 343.0; // m/s in air

        // If speeds are negligible, return normal pitch
        if (Math.abs(sourceVelocity) < 0.05 && Math.abs(listenerVelocity) < 0.05) {
            return 1.0f;
        }

        // Relative speed projection along distance vector
        double relativeSpeed = listenerVelocity - sourceVelocity;

        double factor = (speedOfSound + relativeSpeed) / speedOfSound;
        
        // Clamp shift to prevent absurd chipmunk or sub-bass pitches
        return (float) Math.max(0.5, Math.min(2.0, factor));
    }

    /**
     * Applies random pitch variance inside a harmonized frequency envelope.
     */
    public static float getRandomHarmonizedPitch(float basePitch, float varianceRange) {
        float offset = (float) ((Math.random() - 0.5) * 2.0 * varianceRange);
        return harmonizePitch(basePitch + offset);
    }
}
