#pragma once
#include "tntaura.h"
#include <vector>



struct PlacementPriority {
    TntBlockPos pos;
    double      score;   
    bool        isSupport;
};


std::vector<PlacementPriority> computePlacementOrder(
    const std::vector<TntBlockPos>& cageCandidates,
    const std::vector<TntBlockPos>& solidBlocks,
    double playerX, double playerY, double playerZ
);


std::vector<TntBlockPos> computeSupportChain(
    const TntBlockPos& target,
    const std::vector<TntBlockPos>& solidBlocks,
    double playerX, double playerY, double playerZ,
    double range,
    int maxDepth = 3
);
