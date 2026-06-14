#ifndef RAVEX_PACKETMINE_H
#define RAVEX_PACKETMINE_H

#include <cmath>
#include <vector>
#include <algorithm>

namespace ravex {

struct TargetBlock {
    int x, y, z;
    double dist;
};

/**
 * Finds breakable block targets around the player.
 * Scans in concentric rings for efficient near-to-far discovery.
 */
std::vector<TargetBlock> findMineTargets(
    double px, double py, double pz,
    double range,
    int maxResults
);

/**
 * Estimates the break time for a block based on distance and position.
 * Returns time in milliseconds.
 */
long estimateBreakTime(
    int bx, int by, int bz,
    double px, double py, double pz
);

} // namespace ravex

#endif
