#ifndef RAVEX_FREECAM_MATH_H
#define RAVEX_FREECAM_MATH_H

namespace ravex::hooks::freecam {

    /**
     * Clamps a value between a minimum and maximum bound.
     */
    template <typename T>
    inline T clamp(T val, T min, T max) {
        if (val < min) return min;
        if (val > max) return max;
        return val;
    }

    /**
     * Performs linear interpolation (LERP) between two values based on a weight factor.
     */
    inline double lerp(double start, double end, double factor) {
        return start + (end - start) * factor;
    }

    /**
     * Performs linear interpolation for float values.
     */
    inline float lerpf(float start, float end, float factor) {
        return start + (end - start) * factor;
    }

    /**
     * Normalizes a rotation angle in degrees to be between -180 and 180.
     */
    inline float normalizeAngle(float angle) {
        while (angle <= -180.0f) angle += 360.0f;
        while (angle > 180.0f) angle -= 360.0f;
        return angle;
    }

} // namespace ravex::hooks::freecam

#endif // RAVEX_FREECAM_MATH_H
