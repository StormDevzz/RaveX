#include "trap.h"
#include <cmath>
#include <vector>
#include <algorithm>

static int getFaceIndex(const BlockPos& neighbor, const BlockPos& candidate) {
    int dx = candidate.x - neighbor.x;
    int dy = candidate.y - neighbor.y;
    int dz = candidate.z - neighbor.z;
    if (dy == 1) return 1;  // UP
    if (dy == -1) return 0; // DOWN
    if (dz == -1) return 2; // NORTH
    if (dz == 1) return 3;  // SOUTH
    if (dx == -1) return 4; // WEST
    if (dx == 1) return 5;  // EAST
    return 1; // Fallback
}

static double distSqr(double px, double py, double pz, const BlockPos& pos) {
    double cx = pos.x + 0.5;
    double cy = pos.y + 0.5;
    double cz = pos.z + 0.5;
    double dx = px - cx;
    double dy = (py + 1.62) - cy; // Eye height estimate
    double dz = pz - cz;
    return dx*dx + dy*dy + dz*dz;
}

TrapResult calculateTrap(
    double playerX, double playerY, double playerZ,
    double targetX, double targetY, double targetZ,
    const std::vector<BlockPos>& solidBlocks,
    double range,
    bool roof
) {
    BlockPos targetFeet = { (int)std::floor(targetX), (int)std::floor(targetY), (int)std::floor(targetZ) };

    std::vector<BlockPos> candidates;
    // Feet level
    candidates.push_back({ targetFeet.x,     targetFeet.y,     targetFeet.z - 1 }); // North
    candidates.push_back({ targetFeet.x,     targetFeet.y,     targetFeet.z + 1 }); // South
    candidates.push_back({ targetFeet.x + 1, targetFeet.y,     targetFeet.z });     // East
    candidates.push_back({ targetFeet.x - 1, targetFeet.y,     targetFeet.z });     // West

    // Head level
    candidates.push_back({ targetFeet.x,     targetFeet.y + 1, targetFeet.z - 1 }); // North Head
    candidates.push_back({ targetFeet.x,     targetFeet.y + 1, targetFeet.z + 1 }); // South Head
    candidates.push_back({ targetFeet.x + 1, targetFeet.y + 1, targetFeet.z });     // East Head
    candidates.push_back({ targetFeet.x - 1, targetFeet.y + 1, targetFeet.z });     // West Head

    if (roof) {
        candidates.push_back({ targetFeet.x, targetFeet.y + 2, targetFeet.z });     // Roof
    }

    auto isSolid = [&](const BlockPos& pos) -> bool {
        for (const auto& sb : solidBlocks) {
            if (sb.x == pos.x && sb.y == pos.y && sb.z == pos.z) return true;
        }
        return false;
    };

    const BlockPos offsets[6] = {
        {0, -1, 0}, // DOWN
        {0, 1, 0},  // UP
        {0, 0, -1}, // NORTH
        {0, 0, 1},  // SOUTH
        {-1, 0, 0}, // WEST
        {1, 0, 0}   // EAST
    };

    double rSqr = range * range;

    for (const auto& c : candidates) {
        if (isSolid(c)) continue; // Already placed

        // Check if candidate in range
        if (distSqr(playerX, playerY, playerZ, c) > rSqr) continue;

        // Look for neighbor
        for (const auto& off : offsets) {
            BlockPos n = { c.x + off.x, c.y + off.y, c.z + off.z };
            if (isSolid(n)) {
                return { true, n, getFaceIndex(n, c), c };
            }
        }
    }

    // If no candidate has a solid neighbor, try support blocks
    for (const auto& c : candidates) {
        if (isSolid(c)) continue;

        BlockPos support = { c.x, c.y - 1, c.z };
        if (isSolid(support)) continue;

        if (distSqr(playerX, playerY, playerZ, support) > rSqr) continue;

        // Check if support has a solid neighbor
        for (const auto& off : offsets) {
            BlockPos n = { support.x + off.x, support.y + off.y, support.z + off.z };
            if (isSolid(n)) {
                return { true, n, getFaceIndex(n, support), support };
            }
        }
    }

    return { false, {0,0,0}, 0, {0,0,0} };
}
