#include "nuker.hpp"
#include <cmath>
#include <limits>
#include <algorithm>
#include <cstdint>

// same metric the server uses for reach checks
static double distToBoxSq(double px, double py, double pz, const BlockPos3& pos) {
    double dx = std::max({pos.x - px, 0.0, px - (pos.x + 1.0)});
    double dy = std::max({pos.y - py, 0.0, py - (pos.y + 1.0)});
    double dz = std::max({pos.z - pz, 0.0, pz - (pos.z + 1.0)});
    return dx * dx + dy * dy + dz * dz;
}

std::vector<BlockPos3> findNukerTargets(
    Vec3d eyePos,
    double range,
    int mode,
    const std::vector<BlockPos3>& candidates
) {
    std::vector<BlockPos3> results;
    double rangeSq = range * range;
    double bestDist = std::numeric_limits<double>::max();

    for (const auto& pos : candidates) {
        double distSq;
        if (mode == 0) {
            double cx = pos.x + 0.5;
            double cy = pos.y + 0.5;
            double cz = pos.z + 0.5;
            double dx = cx - eyePos.x;
            double dy = cy - eyePos.y;
            double dz = cz - eyePos.z;
            distSq = dx * dx + dy * dy + dz * dz;
        } else {
            distSq = distToBoxSq(eyePos.x, eyePos.y, eyePos.z, pos);
        }
        if (distSq > rangeSq) continue;
        if (distSq < bestDist) {
            bestDist = distSq;
            results.clear();
            results.push_back(pos);
        }
    }

    return results;
}
