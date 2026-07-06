#pragma once

#include <vector>

struct Vec3d {
    double x, y, z;
};

struct BlockPos3 {
    int x, y, z;
};

std::vector<BlockPos3> findNukerTargets(
    Vec3d eyePos,
    double range,
    int mode,
    const std::vector<BlockPos3>& candidates
);
