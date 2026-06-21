#pragma once

struct ECFarmerResult {
    double breakTimeMs;
    int durabilityLoss;
    bool canBreak;
};

struct ToolInfo {
    bool hasSilkTouch;
    int efficiencyLevel;
    int hasteLevel;
    float destroySpeed;
    int currentDurability;
    int maxDurability;
    bool isValidTool;
};

enum class ECFarmerState {
    IDLE,
    FIND_BREAK,
    BREAKING,
    FIND_PLACE,
    PLACING
};

enum class ToolTier {
    NONE,
    WOODEN,
    STONE,
    IRON,
    DIAMOND,
    NETHERITE
};
