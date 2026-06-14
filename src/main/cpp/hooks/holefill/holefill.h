#ifndef RAVEX_HOLEFILL_H
#define RAVEX_HOLEFILL_H

#include <cmath>
#include <vector>
#include <algorithm>

namespace ravex {

struct Vec3i {
    int x, y, z;
    Vec3i() : x(0), y(0), z(0) {}
    Vec3i(int x, int y, int z) : x(x), y(y), z(z) {}
    double distTo(const Vec3i& o) const {
        double dx = x - o.x, dy = y - o.y, dz = z - o.z;
        return std::sqrt(dx*dx + dy*dy + dz*dz);
    }
    double distToSqr(const Vec3i& o) const {
        double dx = x - o.x, dy = y - o.y, dz = z - o.z;
        return dx*dx + dy*dy + dz*dz;
    }
};

struct HoleCandidate {
    int x, y, z;
    int solidSides;
    double distToPlayer;
};

/**
 * Scans for holes in the block grid around the player.
 * A "hole" is an air pocket at the player's feet level that is:
 *   - Air at (x, y, z)
 *   - Solid block below (floor)
 *   - Air above (open top)
 *   - 3+ solid horizontal neighbors (or 2+ if fillAll)
 *
 * Uses scan-line ray casting for efficient grid traversal.
 */
std::vector<HoleCandidate> findHoles(
    double px, double py, double pz,
    double range,
    int maxResults
);

} // namespace ravex

#endif
