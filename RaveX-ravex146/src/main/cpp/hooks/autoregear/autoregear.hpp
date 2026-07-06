#pragma once
#include <string>
#include <vector>

int calculateRegear(
    const std::vector<std::string>& containerItemIds,
    const std::vector<int>& containerCounts,
    const std::vector<std::string>& targetItemIds,
    const std::vector<int>& targetCounts,
    const std::vector<int>& currentCounts
);
