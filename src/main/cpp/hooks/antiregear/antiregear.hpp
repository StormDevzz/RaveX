#pragma once
#include <vector>

struct BlockCoord {
    int x;
    int y;
    int z;
};

int calculateClosestTarget(
    double playerX, double playerY, double playerZ,
    const std::vector<BlockCoord>& candidates,
    double range
);
