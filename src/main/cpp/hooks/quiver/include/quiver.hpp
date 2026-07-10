#pragma once
#include <string>
#include <vector>

namespace ravex {

struct ActiveEffect {
    std::string id;
    int amplifier;
    double duration;
};

struct ArrowData {
    std::string id;
    int amplifier;
};

int selectBestArrow(
    const std::vector<ActiveEffect>& activeEffects,
    const std::vector<ArrowData>& arrowEffects,
    const std::string& preferredType
);

}
