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


std::vector<TargetBlock> findMineTargets(
    double px, double py, double pz,
    double range,
    int maxResults
);


long estimateBreakTime(
    int bx, int by, int bz,
    double px, double py, double pz
);

} 

#endif
