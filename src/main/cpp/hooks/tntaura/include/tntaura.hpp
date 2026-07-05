#pragma once
#include <vector>
#include <cmath>
#include <cstdint>


struct TntBlockPos {
    int x, y, z;

    bool operator==(const TntBlockPos& o) const {
        return x == o.x && y == o.y && z == o.z;
    }
    bool operator!=(const TntBlockPos& o) const { return !(*this == o); }
};


struct TntAuraConfig {
    double range;
    bool   roof;
    bool   includeBottom;    
    int    gapDirection;     
};


struct TntPlacement {
    bool       valid;
    TntBlockPos neighbor;     
    int        face;          
    TntBlockPos targetBlock;  
};


struct TntAuraResult {
    std::vector<TntPlacement> cagePlacements;   
    TntPlacement              tntPlacement;      
    TntBlockPos               tntPos;            
    TntBlockPos               gapPos;            
    bool                      cageComplete;      
    int                       totalCageSlots;
    int                       filledCageSlots;
};




TntAuraResult calculateTntAuraCage(
    double playerX, double playerY, double playerZ,
    double targetX, double targetY, double targetZ,
    const std::vector<TntBlockPos>& solidBlocks,
    const TntAuraConfig& config
);


TntPlacement findNextCagePlacement(
    double playerX, double playerY, double playerZ,
    double targetX, double targetY, double targetZ,
    const std::vector<TntBlockPos>& solidBlocks,
    const TntAuraConfig& config,
    const TntBlockPos& gapPos
);


TntPlacement calculateTntPlacement(
    double playerX, double playerY, double playerZ,
    const TntBlockPos& gapPos,
    const std::vector<TntBlockPos>& solidBlocks,
    double range
);
