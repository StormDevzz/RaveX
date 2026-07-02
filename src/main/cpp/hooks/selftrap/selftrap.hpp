#pragma once
#include <vector>

struct BlockPos {
    int x;
    int y;
    int z;
};

struct SelfTrapResult {
    bool found;
    BlockPos neighbor;
    int face;
    BlockPos targetBlock;
};

SelfTrapResult calculateSelfTrap(
    double playerX, double playerY, double playerZ,
    const std::vector<BlockPos>& solidBlocks,
    double range,
    int mode
);
