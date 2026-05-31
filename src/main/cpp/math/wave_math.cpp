#include "wave_math.h"
#include <cmath>

namespace ravex::math {
    float calculateWave(float time, float x, float z) {
        return sinf(time) * 0.012f;
    }
}
