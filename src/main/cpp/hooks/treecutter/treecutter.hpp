#pragma once
#include <vector>

struct LogPos {
    double x, y, z;
};

struct TreeCutterResult {
    bool found;
    double x, y, z;
};

TreeCutterResult findBestLog(
    double playerX, double playerY, double playerZ,
    const std::vector<LogPos>& candidates
);
