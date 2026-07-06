#include "include/AddonMath.hpp"

namespace ravex {
namespace addon {

double AddonMath::clamp(double val, double min, double max) {
    if (val < min) return min;
    if (val > max) return max;
    return val;
}

float AddonMath::lerp(float a, float b, float t) {
    return a + t * (b - a);
}

}
}
