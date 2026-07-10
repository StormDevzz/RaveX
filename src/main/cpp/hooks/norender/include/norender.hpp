#ifndef RAVEX_NORENDER_H
#define RAVEX_NORENDER_H

#include <vector>

namespace ravex::norender {
    bool shouldCull(double x, double y, double z, double camX, double camY, double camZ, double maxDist);
    int optimizeBudget(int activeCount, int currentFps, int minFps);
    std::vector<float> optimizeFog(float envStart, float envEnd, float rdStart, float rdEnd, float skyEnd, float cloudEnd);
}

#endif
