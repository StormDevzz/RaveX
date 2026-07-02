#pragma once
#include <vector>
#include <cmath>

struct BlockPos {
    int x, y, z;

    bool operator==(const BlockPos& other) const {
        return x == other.x && y == other.y && z == other.z;
    }
};

struct TrapResult {
    bool found;
    BlockPos neighbor;
    int face;
    BlockPos targetBlock;
};

TrapResult calculateTrap(
    double playerX, double playerY, double playerZ,
    double targetX, double targetY, double targetZ,
    const std::vector<BlockPos>& solidBlocks,
    double range,
    bool roof
);
