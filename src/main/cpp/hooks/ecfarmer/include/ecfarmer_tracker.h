#pragma once

struct TrackedBlock {
    int x;
    int y;
    int z;
    float distance;
    unsigned long lastSeenTick;
};

struct BreakProgress {
    unsigned long startTime;
    bool isBreaking;
    float progress;
};

void trackerInit();
void trackerReset();
void trackerAddBlock(int x, int y, int z, unsigned long tick);
bool trackerIsTracked(int x, int y, int z);
void trackerRemoveBlock(int x, int y, int z);
