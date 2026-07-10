#ifndef RAVEX_AUTOCLICKER_H
#define RAVEX_AUTOCLICKER_H

#include <cstdint>
#include <random>

namespace ravex {

inline std::mt19937& getRng() {
    static std::random_device rd;
    static std::mt19937 gen(rd());
    return gen;
}

inline int64_t calculateClickDelay(double minCps, double maxCps, bool randomize) {
    double cps;
    if (randomize) {
        std::uniform_real_distribution<double> dist(minCps, maxCps);
        cps = dist(getRng());
    } else {
        cps = (minCps + maxCps) * 0.5;
    }
    if (cps <= 0.0) cps = 1.0;
    int64_t delay = static_cast<int64_t>(1000.0 / cps);
    if (randomize) {
        std::uniform_int_distribution<int> jitter(0, static_cast<int>(delay * 0.15));
        delay += jitter(getRng());
    }
    return delay;
}

}

#endif
