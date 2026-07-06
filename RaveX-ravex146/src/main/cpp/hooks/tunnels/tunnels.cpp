#include "tunnels.hpp"
#include <cmath>
#include <cstring>

namespace ravex {

std::vector<int> scanTunnels(double px, double py, double pz, double range, int maxY, int minY) {
    std::vector<int> results;
    int r = static_cast<int>(range);
    int cx = static_cast<int>(std::floor(px));
    int cz = static_cast<int>(std::floor(pz));

    for (int x = cx - r; x <= cx + r; x++) {
        for (int z = cz - r; z <= cz + r; z++) {
            for (int y = minY; y <= maxY; y++) {
                double dx = x - px;
                double dz = z - pz;
                if (std::sqrt(dx * dx + dz * dz) > range) continue;
                results.push_back(x);
                results.push_back(y);
                results.push_back(z);
            }
        }
    }

    return results;
}

}
