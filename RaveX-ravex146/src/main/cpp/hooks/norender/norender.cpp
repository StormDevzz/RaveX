#include "norender.hpp"

namespace ravex::norender {
    bool shouldCull(double x, double y, double z, double camX, double camY, double camZ, double maxDist) {
        double dx = x - camX;
        double dy = y - camY;
        double dz = z - camZ;
        double distSq = dx * dx + dy * dy + dz * dz;
        return distSq > (maxDist * maxDist);
    }

    int optimizeBudget(int activeCount, int currentFps, int minFps) {
        if (minFps <= 0) return activeCount;
        if (currentFps < minFps) {
            double ratio = (double)currentFps / (double)minFps;
            if (ratio < 0.1) ratio = 0.1;
            return (int)(activeCount * ratio);
        }
        return activeCount;
    }

    std::vector<float> optimizeFog(float envStart, float envEnd, float rdStart, float rdEnd, float skyEnd, float cloudEnd) {
        return { 999999.0f, 999999.0f, 999999.0f, 999999.0f, 999999.0f, 999999.0f };
    }
}
