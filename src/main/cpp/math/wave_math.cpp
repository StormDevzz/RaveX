#include "wave_math.h"
#include <cmath>

namespace ravex::math {
    float calculateWave(float time, float x, float z) {
        float wave1 = sinf(time + x * 0.5f) * cosf(time + z * 0.5f);
        float wave2 = sinf(time * 0.5f + (x + z) * 0.2f) * 0.5f;
        return (wave1 + wave2) * 0.02f;
    }
}
