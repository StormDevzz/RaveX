#include "ecfarmer.h"
#include <cmath>

ToolInfo analyzeTool(const char* toolId, int efficiency, int haste, int durability, int maxDura) {
    ToolInfo info;
    info.efficiencyLevel = efficiency;
    info.hasteLevel = haste;
    info.currentDurability = durability;
    info.maxDurability = maxDura;
    info.isValidTool = false;
    info.hasSilkTouch = false;
    info.destroySpeed = 1.0f;

    if (toolId == nullptr) return info;

    if (!isPickaxe(toolId)) return info;

    info.isValidTool = true;
    info.destroySpeed = calcDestroySpeed(toolId);

    if (info.efficiencyLevel > 0) {
        info.destroySpeed += info.efficiencyLevel * info.efficiencyLevel + 1.0f;
    }

    if (info.hasteLevel > 0) {
        info.destroySpeed *= (1.0f + info.hasteLevel * 0.2f);
    }

    return info;
}

double calcBreakTime(const ToolInfo& info) {
    double breakTime = 22.5 / info.destroySpeed / 30.0;
    return (breakTime * 1000.0) + 50.0;
}

int calcDurabilityLoss(const ToolInfo& info) {
    if (!info.isValidTool) return -1;
    return 1;
}

bool canBreakBlock(const ToolInfo& info, float hardness) {
    if (!info.isValidTool) return false;
    if (info.currentDurability <= 0) return false;
    return info.destroySpeed >= hardness;
}
