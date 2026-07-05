#pragma once

struct WitherRoseResult {
    bool found;
    int neighborX, neighborY, neighborZ;
    int face;
    int targetX, targetY, targetZ;
};

WitherRoseResult calculateWitherRose(
    double playerX, double playerY, double playerZ,
    double targetX, double targetY, double targetZ,
    double range,
    bool targetFeetIsReplaceable,
    bool supportBlockIsSolid
);
