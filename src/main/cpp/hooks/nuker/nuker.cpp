#include "nuker.h"
#include <cmath>
#include <limits>
#include <cstdint>

std::vector<BlockPos3> findNukerTargets(
    Vec3d eyePos,
    double range,
    int mode,
    const std::vector<BlockPos3>& candidates
) {
    std::vector<BlockPos3> results;
    double rangeSq = range * range;
    double bestDist = std::numeric_limits<double>::max();

    if (mode == 0) {
        for (const auto& pos : candidates) {
            double cx = pos.x + 0.5;
            double cy = pos.y + 0.5;
            double cz = pos.z + 0.5;
            double dx = cx - eyePos.x;
            double dy = cy - eyePos.y;
            double dz = cz - eyePos.z;
            double distSq = dx * dx + dy * dy + dz * dz;

            if (distSq <= rangeSq && distSq < bestDist) {
                bestDist = distSq;
                results.clear();
                results.push_back(pos);
            }
        }
    } else {
        for (const auto& pos : candidates) {
            double cx = pos.x + 0.5;
            double cy = pos.y + 0.5;
            double cz = pos.z + 0.5;
            double dx = cx - eyePos.x;
            double dy = cy - eyePos.y;
            double dz = cz - eyePos.z;
            double dist = std::sqrt(dx * dx + dy * dy + dz * dz);

            if (dist <= range && dist < bestDist) {
                bestDist = dist;
                results.clear();
                results.push_back(pos);
            }
        }
    }

    return results;
}
