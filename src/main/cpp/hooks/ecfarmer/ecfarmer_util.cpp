#include "include/ecfarmer_util.h"
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
}

bool isEnderChest(const char* blockId) {
    if (blockId == nullptr) return false;
    return std::strstr(blockId, "ender_chest") != nullptr;
}
