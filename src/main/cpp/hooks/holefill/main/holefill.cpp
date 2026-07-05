#include "holefill.hpp"
#include <cmath>
#include <cstring>
#include <vector>
#include <algorithm>

namespace ravex {

static const int DIR_NORTH = 0;
static const int DIR_SOUTH = 1;
static const int DIR_EAST  = 2;
static const int DIR_WEST  = 3;

static const int DIR_DX[] = { 0,  0,  1, -1};
static const int DIR_DZ[] = {-1,  1,  0,  0};

struct BlockInfo {
    bool solid;
};


std::vector<HoleCandidate> findHoles(
    double px, double py, double pz,
    double range,
    int maxResults)
{
    std::vector<HoleCandidate> results;
    results.reserve(maxResults < 1 ? 32 : maxResults);

    int px_i = static_cast<int>(std::round(px));
    int pz_i = static_cast<int>(std::round(pz));

    
    int feetY = static_cast<int>(std::floor(py));
    int range_i = static_cast<int>(std::ceil(range));

    
    auto isDuplicate = [&](int x, int y, int z) -> bool {
        for (const auto& h : results) {
            if (h.x == x && h.y == y && h.z == z) return true;
        }
        return false;
    };

    
    for (int ring = 0; ring <= range_i; ring++) {
        int ringStart = (ring == 0) ? 0 : ring;

        
        for (int dx = -ringStart; dx <= ringStart; dx++) {
            for (int dz = -ringStart; dz <= ringStart; dz++) {
                
                if (ring > 0 && std::abs(dx) < ring && std::abs(dz) < ring) continue;

                
                double dist = std::sqrt(double(dx*dx + dz*dz));
                if (dist > range) continue;

                
                for (int dy = -1; dy <= 1; dy++) {
                    int x = px_i + dx;
                    int y = feetY + dy;
                    int z = pz_i + dz;

                    if (isDuplicate(x, y, z)) continue;

                    
                    double d3 = std::sqrt(
                        (x - px) * (x - px) +
                        (y - py) * (y - py) +
                        (z - pz) * (z - pz)
                    );
                    if (d3 > range) continue;

                    
                    
                    HoleCandidate cand;
                    cand.x = x;
                    cand.y = y;
                    cand.z = z;
                    cand.solidSides = 0; 
                    cand.distToPlayer = d3;
                    results.push_back(cand);

                    if (maxResults > 0 && results.size() >= static_cast<size_t>(maxResults)) {
                        goto done;
                    }
                }
            }
        }
    }

done:
    
    std::sort(results.begin(), results.end(),
        [](const HoleCandidate& a, const HoleCandidate& b) {
            return a.distToPlayer < b.distToPlayer;
        });

    return results;
}

} 
