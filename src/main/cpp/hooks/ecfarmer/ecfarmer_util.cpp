#include "include/ecfarmer_util.h"
<<<<<<< HEAD
#include <cstring>
#include <string_view>

float calcDestroySpeed(const char* toolId) {
    if (toolId == nullptr) return 1.0f;

    std::string_view id(toolId);

    if (id.find("netherite") != std::string_view::npos) return 9.0f;
    if (id.find("diamond") != std::string_view::npos) return 8.0f;
    if (id.find("iron") != std::string_view::npos) return 6.0f;
    if (id.find("stone") != std::string_view::npos) return 4.0f;
    if (id.find("wooden") != std::string_view::npos) return 2.0f;
    if (id.find("gold") != std::string_view::npos) return 12.0f;

    return 1.0f;
}

int getToolTier(const char* toolId) {
    if (toolId == nullptr) return 0;

    std::string_view id(toolId);

    if (id.find("netherite") != std::string_view::npos) return 4;
    if (id.find("diamond") != std::string_view::npos) return 3;
    if (id.find("iron") != std::string_view::npos) return 2;
    if (id.find("stone") != std::string_view::npos) return 1;
    if (id.find("wooden") != std::string_view::npos) return 0;
    if (id.find("gold") != std::string_view::npos) return 0;

    return 0;
}

bool isPickaxe(const char* toolId) {
    if (toolId == nullptr) return false;
    return std::strstr(toolId, "pickaxe") != nullptr;
=======
#include "include/ecfarmer_types.h"
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
>>>>>>> d5789b70550118b35e864d8afa6cd32033b90fc8
}

bool isEnderChest(const char* blockId) {
    if (blockId == nullptr) return false;
<<<<<<< HEAD
    return std::strstr(blockId, "ender_chest") != nullptr;
=======
    return strstr(blockId, "ender_chest") != nullptr;
>>>>>>> d5789b70550118b35e864d8afa6cd32033b90fc8
}
