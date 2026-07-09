#include "packetmine.hpp"

namespace ravex {

std::vector<TargetBlock> findMineTargets(
    double px, double py, double pz,
    double range,
    int maxResults)
{
    std::vector<TargetBlock> results;
    int px_i = static_cast<int>(std::round(px));
    int pz_i = static_cast<int>(std::round(pz));
    int py_i = static_cast<int>(std::floor(py));
    int range_i = static_cast<int>(std::ceil(range));

    auto isDup = [&](int x, int y, int z) -> bool {
        for (auto& t : results)
            if (t.x == x && t.y == y && t.z == z) return true;
        return false;
    };

    
    for (int ring = 0; ring <= range_i; ring++) {
        int rs = (ring == 0) ? 0 : ring;

        for (int dx = -rs; dx <= rs; dx++) {
            for (int dz = -rs; dz <= rs; dz++) {
                if (ring > 0 && std::abs(dx) < ring && std::abs(dz) < ring) continue;
                if (std::sqrt(double(dx*dx + dz*dz)) > range) continue;

                for (int dy = -range_i; dy <= range_i; dy++) {
                    int x = px_i + dx;
                    int y = py_i + dy;
                    int z = pz_i + dz;

                    if (isDup(x, y, z)) continue;

                    double d3 = std::sqrt(
                        (x - px) * (x - px) +
                        (y - py) * (y - py) +
                        (z - pz) * (z - pz)
                    );
                    if (d3 > range) continue;

                    TargetBlock tb;
                    tb.x = x; tb.y = y; tb.z = z;
                    tb.dist = d3;
                    results.push_back(tb);

                    if (maxResults > 0 && results.size() >= static_cast<size_t>(maxResults))
                        goto done;
                }
            }
        }
    }

done:
    std::sort(results.begin(), results.end(),
        [](const TargetBlock& a, const TargetBlock& b) { return a.dist < b.dist; });
    return results;
}

long estimateBreakTime(int bx, int by, int bz, double px, double py, double pz) {
    double dist = std::sqrt(
        (bx - px) * (bx - px) +
        (by - py) * (by - py) +
        (bz - pz) * (bz - pz)
    );
    
    return static_cast<long>(50.0 + dist * 10.0);
}

bool canSee(
    double ex, double ey, double ez,
    double tx, double ty, double tz,
    const std::vector<Vec3i>& solidBlocks)
{
    double dx = tx - ex;
    double dy = ty - ey;
    double dz = tz - ez;
    double len = std::sqrt(dx * dx + dy * dy + dz * dz);
    if (len < 0.001) return true;

    double nx = dx / len;
    double ny = dy / len;
    double nz = dz / len;

    int steps = std::max(8, static_cast<int>(len / 0.5));
    for (int i = 1; i < steps; i++) {
        double t = static_cast<double>(i) / steps * len;
        double px = ex + nx * t;
        double py = ey + ny * t;
        double pz = ez + nz * t;

        int bx = static_cast<int>(std::floor(px));
        int by = static_cast<int>(std::floor(py));
        int bz = static_cast<int>(std::floor(pz));

        for (const auto& blk : solidBlocks) {
            if (blk.x == bx && blk.y == by && blk.z == bz)
                return false;
        }
    }
    return true;
}

void filterVisibleBlocks(
    const std::vector<Vec3i>& candidates,
    const std::vector<Vec3i>& solidBlocks,
    double ex, double ey, double ez,
    std::vector<Vec3i>& out)
{
    out.clear();
    out.reserve(candidates.size());
    for (const auto& c : candidates) {
        if (canSee(ex, ey, ez, c.x + 0.5, c.y + 0.5, c.z + 0.5, solidBlocks)) {
            out.push_back(c);
        }
    }
}

} 
