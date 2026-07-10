#ifndef RAVEX_AUTODROP_H
#define RAVEX_AUTODROP_H

#include <cmath>
#include <vector>

namespace ravex {

struct PlaceTarget {
    int x, y, z;
    double score;
};

inline double calcDropAngle(double px, double pz, double tx, double tz) {
    return std::atan2(tx - px, pz - tz);
}

inline std::vector<PlaceTarget> findDropTargets(
    double px, double py, double pz,
    double range, int maxResults)
{
    std::vector<PlaceTarget> targets;
    int pxi = static_cast<int>(std::floor(px));
    int pyi = static_cast<int>(std::floor(py));
    int pzi = static_cast<int>(std::floor(pz));

    int r = static_cast<int>(std::ceil(range));
    for (int dx = -r; dx <= r; dx++) {
        for (int dz = -r; dz <= r; dz++) {
            double dist = std::sqrt(dx*dx + dz*dz);
            if (dist > range) continue;
            PlaceTarget t;
            t.x = pxi + dx;
            t.y = pyi + 1;
            t.z = pzi + dz;
            t.score = dist;
            targets.push_back(t);
        }
    }

    if (targets.size() > static_cast<size_t>(maxResults))
        targets.resize(maxResults);

    return targets;
}

}

#endif
