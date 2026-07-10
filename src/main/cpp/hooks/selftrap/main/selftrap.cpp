#include "selftrap.hpp"
#include <cmath>
#include <vector>
#include <algorithm>

static int getFaceIndex(const BlockPos& neighbor, const BlockPos& candidate) {
    int dx = candidate.x - neighbor.x;
    int dy = candidate.y - neighbor.y;
    int dz = candidate.z - neighbor.z;
    if (dy == 1) return 1;
    if (dy == -1) return 0;
    if (dz == -1) return 2;
    if (dz == 1) return 3;
    if (dx == -1) return 4;
    if (dx == 1) return 5;
    return 1;
}

static double distSqr(double px, double py, double pz, const BlockPos& pos) {
    double cx = pos.x + 0.5;
    double cy = pos.y + 0.5;
    double cz = pos.z + 0.5;
    double dx = px - cx;
    double dy = (py + 1.62) - cy;
    double dz = pz - cz;
    return dx*dx + dy*dy + dz*dz;
}

SelfTrapResult calculateSelfTrap(
    double playerX, double playerY, double playerZ,
    const std::vector<BlockPos>& solidBlocks,
    double range,
    int mode
) {
    BlockPos pf = { (int)std::floor(playerX), (int)std::floor(playerY), (int)std::floor(playerZ) };

    std::vector<BlockPos> candidates;


    if (mode == 0 || mode == 1) {

        candidates.push_back({ pf.x,     pf.y,     pf.z - 1 });
        candidates.push_back({ pf.x,     pf.y,     pf.z + 1 });
        candidates.push_back({ pf.x + 1, pf.y,     pf.z });
        candidates.push_back({ pf.x - 1, pf.y,     pf.z });


        candidates.push_back({ pf.x,     pf.y + 1, pf.z - 1 });
        candidates.push_back({ pf.x,     pf.y + 1, pf.z + 1 });
        candidates.push_back({ pf.x + 1, pf.y + 1, pf.z });
        candidates.push_back({ pf.x - 1, pf.y + 1, pf.z });
    }

    if (mode == 0 || mode == 2) {
        candidates.push_back({ pf.x, pf.y + 2, pf.z });
    }

    auto isSolid = [&](const BlockPos& pos) -> bool {

        if (pos.x == pf.x && pos.z == pf.z && (pos.y == pf.y || pos.y == pf.y + 1)) {
            return false;
        }
        for (const auto& sb : solidBlocks) {
            if (sb.x == pos.x && sb.y == pos.y && sb.z == pos.z) return true;
        }
        return false;
    };

    const BlockPos offsets[6] = {
        {0, -1, 0},
        {0, 1, 0},
        {0, 0, -1},
        {0, 0, 1},
        {-1, 0, 0},
        {1, 0, 0}
    };

    double rSqr = range * range;


    for (const auto& c : candidates) {
        if (isSolid(c)) continue;

        if (distSqr(playerX, playerY, playerZ, c) > rSqr) continue;

        for (const auto& off : offsets) {
            BlockPos n = { c.x + off.x, c.y + off.y, c.z + off.z };
            if (isSolid(n)) {
                return { true, n, getFaceIndex(n, c), c };
            }
        }
    }


    for (const auto& c : candidates) {
        if (isSolid(c)) continue;

        BlockPos support = { c.x, c.y - 1, c.z };
        if (isSolid(support)) continue;

        if (distSqr(playerX, playerY, playerZ, support) > rSqr) continue;

        for (const auto& off : offsets) {
            BlockPos n = { support.x + off.x, support.y + off.y, support.z + off.z };
            if (isSolid(n)) {
                return { true, n, getFaceIndex(n, support), support };
            }
        }
    }

    return { false, {0,0,0}, 0, {0,0,0} };
}
