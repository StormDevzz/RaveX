#include "autoregear.h"

int calculateRegear(
    const std::vector<std::string>& containerItemIds,
    const std::vector<int>& containerCounts,
    const std::vector<std::string>& targetItemIds,
    const std::vector<int>& targetCounts,
    const std::vector<int>& currentCounts
) {
    for (size_t i = 0; i < targetItemIds.size(); ++i) {
        if (currentCounts[i] < targetCounts[i]) {
            const std::string& targetId = targetItemIds[i];
            for (size_t slot = 0; slot < containerItemIds.size(); ++slot) {
                const std::string& containerId = containerItemIds[slot];
                if (containerId == targetId && containerCounts[slot] > 0) {
                    return static_cast<int>(slot);
                }
                if (targetId == "minecraft:enchanted_golden_apple" && containerId == "minecraft:golden_apple" && containerCounts[slot] > 0) {
                    return static_cast<int>(slot);
                }
            }
        }
    }
    return -1;
}
