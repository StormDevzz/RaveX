#include "voidesp.hpp"
#include <cmath>

namespace ravex {

std::vector<int> scanVoid(double px, double pz, double range, int height, bool floorOnly) {
    std::vector<int> results;
    int r = static_cast<int>(range);
    int cx = static_cast<int>(std::floor(px));
    int cz = static_cast<int>(std::floor(pz));

    for (int x = cx - r; x <= cx + r; x++) {
        for (int z = cz - r; z <= cz + r; z++) {
            double dx = x - px;
            double dz = z - pz;
            if (std::sqrt(dx * dx + dz * dz) > range) continue;
            results.push_back(x);
            results.push_back(z);
        }
    }

    return results;
}

}
