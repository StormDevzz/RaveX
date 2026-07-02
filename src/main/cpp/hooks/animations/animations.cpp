#include "animations.hpp"
#include <cmath>

namespace ravex {

float updateAnimation(float current, float speed, float walkSpeed, bool smooth) {
    float increment = speed * walkSpeed;
    if (smooth) {
        increment *= 0.75f;
    }
    current += increment;
    if (current > 1.0f) {
        current -= 1.0f;
    }
    return current;
}

}
