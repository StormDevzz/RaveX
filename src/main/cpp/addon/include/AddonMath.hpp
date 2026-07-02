#pragma once

namespace ravex {
namespace addon {

class AddonMath {
public:
    static double clamp(double val, double min, double max);
    static float lerp(float a, float b, float t);
};

}
}
