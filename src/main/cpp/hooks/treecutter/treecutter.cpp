#include "treecutter.h"
#include <cmath>
#include <limits>

TreeCutterResult findBestLog(
    double playerX, double playerY, double playerZ,
    const std::vector<LogPos>& candidates
) {
    TreeCutterResult result = { false, 0.0, 0.0, 0.0 };
    if (candidates.empty()) {
        return result;
    }

    double bestDistSqr = std::numeric_limits<double>::max();
    LogPos bestLog = { 0.0, 0.0, 0.0 };

    for (const auto& c : candidates) {
        
        double dx = (c.x + 0.5) - playerX;
        double dy = (c.y + 0.5) - playerY;
        double dz = (c.z + 0.5) - playerZ;
        double distSqr = dx * dx + dy * dy + dz * dz;

        if (distSqr < bestDistSqr) {
            bestDistSqr = distSqr;
            bestLog = c;
            result.found = true;
        }
    }

    if (result.found) {
        result.x = bestLog.x;
        result.y = bestLog.y;
        result.z = bestLog.z;
    }

    return result;
}
