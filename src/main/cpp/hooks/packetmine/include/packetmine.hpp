#ifndef RAVEX_PACKETMINE_H
#define RAVEX_PACKETMINE_H

#include <cmath>
#include <vector>
#include <algorithm>
#include <cstdint>

namespace ravex {

struct TargetBlock {
    int x, y, z;
    double dist;
};

struct Vec3i {
    int x, y, z;
};

std::vector<TargetBlock> findMineTargets(
    double px, double py, double pz,
    double range,
    int maxResults
);

long estimateBreakTime(
    int bx, int by, int bz,
    double px, double py, double pz
);

bool canSee(
    double ex, double ey, double ez,
    double tx, double ty, double tz,
    const std::vector<Vec3i>& solidBlocks
);

void filterVisibleBlocks(
    const std::vector<Vec3i>& candidates,
    const std::vector<Vec3i>& solidBlocks,
    double ex, double ey, double ez,
    std::vector<Vec3i>& out
);

}

#endif
