#include "antiregear.hpp"
#include <cmath>

int calculateClosestTarget(
    double playerX, double playerY, double playerZ,
    const std::vector<BlockCoord>& candidates,
    double range
) {
    double bestDistSq = range * range;
    int bestIdx = -1;

    for (size_t i = 0; i < candidates.size(); ++i) {
        double dx = (candidates[i].x + 0.5) - playerX;
        double dy = (candidates[i].y + 0.5) - playerY;
        double dz = (candidates[i].z + 0.5) - playerZ;
        double distSq = dx * dx + dy * dy + dz * dz;

        if (distSq < bestDistSq) {
            bestDistSq = distSq;
            bestIdx = static_cast<int>(i);
        }
    }

    return bestIdx;
}
