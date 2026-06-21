#include "ecfarmer.h"

ECFarmerResult calculateBreak(const char* toolId, int efficiency, int haste, int durability, int maxDura) {
    ECFarmerResult result;
    ToolInfo info = analyzeTool(toolId, efficiency, haste, durability, maxDura);

    if (!info.isValidTool) {
        result.canBreak = false;
        result.breakTimeMs = -1.0;
        result.durabilityLoss = -1;
        return result;
    }

    result.canBreak = canBreakBlock(info, 3.0f);
    result.breakTimeMs = calcBreakTime(info);
    result.durabilityLoss = calcDurabilityLoss(info);
    return result;
}
