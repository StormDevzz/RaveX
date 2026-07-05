#include "noslow.hpp"

float getBypassFriction(const std::string& blockId, float defaultFriction) {
    if (blockId == "minecraft:slime_block" || 
        blockId == "minecraft:honey_block" || 
        blockId == "minecraft:soul_sand") {
        return 0.6f; 
    }
    return defaultFriction;
}
