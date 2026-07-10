#ifndef RAVEX_PACKETMINE_H
#define RAVEX_PACKETMINE_H

#include <cmath>
#include <vector>
#include <algorithm>
<<<<<<< HEAD
#include <cstdint>
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3

namespace ravex {

struct TargetBlock {
    int x, y, z;
    double dist;
};

<<<<<<< HEAD
struct Vec3i {
    int x, y, z;
};
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3

std::vector<TargetBlock> findMineTargets(
    double px, double py, double pz,
    double range,
    int maxResults
);

<<<<<<< HEAD
=======

>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
long estimateBreakTime(
    int bx, int by, int bz,
    double px, double py, double pz
);

<<<<<<< HEAD
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

=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}

#endif
