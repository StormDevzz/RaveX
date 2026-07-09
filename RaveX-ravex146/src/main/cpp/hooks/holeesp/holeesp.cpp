#include "holeesp.hpp"
#include <cmath>

namespace ravex {

std::vector<int> scanHoles(double px, double py, double pz, double range) {
    std::vector<int> results;
    int r = static_cast<int>(range);
    int cx = static_cast<int>(std::floor(px));
    int cz = static_cast<int>(std::floor(pz));
    int cy = static_cast<int>(std::floor(py));

    for (int x = cx - r; x <= cx + r; x++) {
        for (int z = cz - r; z <= cz + r; z++) {
            for (int y = cy - 1; y <= cy + 1; y++) {
                double dx = x - px;
                double dz = z - pz;
                if (std::sqrt(dx * dx + dz * dz) > range) continue;
                results.push_back(x);
                results.push_back(y);
                results.push_back(z);
                results.push_back(0);
            }
        }
    }

    return results;
}

}
