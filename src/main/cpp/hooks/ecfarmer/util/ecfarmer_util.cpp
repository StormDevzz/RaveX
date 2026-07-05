#include "ecfarmer_types.hpp"
#include "ecfarmer_util.hpp"
#include <cstring>

bool isPickaxe(const char* toolId) {
    if (toolId == nullptr) return false;
    return strstr(toolId, "pickaxe") != nullptr;
}

int getToolTier(const char* toolId) {
    if (toolId == nullptr) return (int)ToolTier::NONE;
    if (strstr(toolId, "netherite")) return (int)ToolTier::NETHERITE;
    if (strstr(toolId, "diamond")) return (int)ToolTier::DIAMOND;
    if (strstr(toolId, "iron")) return (int)ToolTier::IRON;
    if (strstr(toolId, "stone")) return (int)ToolTier::STONE;
    if (strstr(toolId, "wooden")) return (int)ToolTier::WOODEN;
    return (int)ToolTier::NONE;
}

float calcDestroySpeed(const char* toolId) {
    int tier = getToolTier(toolId);
    switch (tier) {
        case (int)ToolTier::NETHERITE: return 9.0f;
        case (int)ToolTier::DIAMOND:   return 8.0f;
        case (int)ToolTier::IRON:      return 6.0f;
        case (int)ToolTier::STONE:     return 4.0f;
        case (int)ToolTier::WOODEN:    return 2.0f;
        default:                       return 1.0f;
    }
}

bool isEnderChest(const char* blockId) {
    if (blockId == nullptr) return false;
    return strstr(blockId, "ender_chest") != nullptr;
}
