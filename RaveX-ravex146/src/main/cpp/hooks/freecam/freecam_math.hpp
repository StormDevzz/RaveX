#ifndef RAVEX_FREECAM_MATH_H
#define RAVEX_FREECAM_MATH_H

namespace ravex::hooks::freecam {


    template <typename T>
    inline T clamp(T val, T min, T max) {
        if (val < min) return min;
        if (val > max) return max;
        return val;
    }


    inline double lerp(double start, double end, double factor) {
        return start + (end - start) * factor;
    }


    inline float lerpf(float start, float end, float factor) {
        return start + (end - start) * factor;
    }


    inline float normalizeAngle(float angle) {
        while (angle <= -180.0f) angle += 360.0f;
        while (angle > 180.0f) angle -= 360.0f;
        return angle;
    }

}

#endif
