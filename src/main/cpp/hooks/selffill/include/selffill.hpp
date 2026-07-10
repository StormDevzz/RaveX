#ifndef RAVEX_SELFFILL_H
#define RAVEX_SELFFILL_H

#include <cmath>

namespace ravex {

inline double calcPlaceAngle(double px, double pz, double bx, double bz) {
    return std::atan2(bx + 0.5 - px, pz - (bz + 0.5));
}

}

#endif
