#ifndef RAVEX_KILLAURA_H
#define RAVEX_KILLAURA_H

#include <algorithm>

namespace ravex {

inline constexpr double KILLAURA_JUMP_VELOCITY = 0.42;
inline constexpr double KILLAURA_GRAVITY = 0.08;
inline constexpr double KILLAURA_DRAG = 0.98;
inline constexpr double KILLAURA_MIN_SERVER_FALL = 0.3;

struct CritWindow {
    bool fire;
    double quality;
    double serverFall;
};

inline CritWindow evaluateCritWindow(double fall, double chargeTrue, double minFall) {
    double need = minFall > KILLAURA_MIN_SERVER_FALL ? minFall : KILLAURA_MIN_SERVER_FALL;
    double quality = fall <= 0.0 ? 0.0 : (fall > 1.0 ? 1.0 : fall);
    bool fire = (fall > need);
    return {fire, quality, fall};
}

inline double fallAfterTicks(double fall, double velY, int ticks) {
    double v = velY;
    double f = fall;
    for (int i = 0; i < ticks; i++) {
        v = (v - KILLAURA_GRAVITY) * KILLAURA_DRAG;
        if (v < 0.0) f += -v;
    }
    return f;
}

}

#endif
