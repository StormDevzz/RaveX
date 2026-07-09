#pragma once
#include "ecfarmer_types.hpp"

ToolInfo analyzeTool(const char* toolId, int efficiency, int haste, int durability, int maxDura);

double calcBreakTime(const ToolInfo& info);

int calcDurabilityLoss(const ToolInfo& info);

bool canBreakBlock(const ToolInfo& info, float hardness);
