#pragma once

struct ECFarmerResult {
    double breakTimeMs;
    int durabilityLoss;
    bool canBreak;
};

ECFarmerResult calculateBreak(const char* toolId, int efficiency, int haste, int durability, int maxDura);
