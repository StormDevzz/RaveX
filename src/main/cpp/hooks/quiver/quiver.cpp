#include "quiver.h"
#include <algorithm>

namespace ravex {

int selectBestArrow(
    const std::vector<ActiveEffect>& activeEffects,
    const std::vector<ArrowData>& arrowEffects,
    const std::string& preferredType
) {
    int bestIndex = -1;
    double bestScore = -999.0;

    std::string pref = preferredType;
    std::transform(pref.begin(), pref.end(), pref.begin(), ::tolower);

    for (size_t i = 0; i < arrowEffects.size(); i++) {
        const auto& arrow = arrowEffects[i];
        if (arrow.id.empty()) continue;

        std::string eName = arrow.id;
        std::transform(eName.begin(), eName.end(), eName.begin(), ::tolower);

        bool match = false;
        double typeScore = 0.0;

        if (pref == "strength" && eName.find("strength") != std::string::npos) {
            match = true;
            typeScore = 1000.0;
        } else if (pref == "speed" && (eName.find("swiftness") != std::string::npos || eName.find("speed") != std::string::npos)) {
            match = true;
            typeScore = 800.0;
        } else if (pref == "healing" && (eName.find("instant_health") != std::string::npos || eName.find("healing") != std::string::npos || eName.find("regeneration") != std::string::npos || eName.find("regen") != std::string::npos)) {
            match = true;
            typeScore = 600.0;
        } else if (pref == "fire resistance" && (eName.find("fire_resistance") != std::string::npos || eName.find("fireres") != std::string::npos)) {
            match = true;
            typeScore = 400.0;
        }

        if (!match) continue;

        
        double score = typeScore + arrow.amplifier * 10.0;

        
        for (const auto& eff : activeEffects) {
            bool effMatch = (eff.id == eName) || 
                          (eff.id.find(eName) != std::string::npos) || 
                          (eName.find(eff.id) != std::string::npos);
            if (effMatch && eff.amplifier >= arrow.amplifier && eff.duration > 3.0) {
                score -= 200.0;
            }
        }

        if (score > bestScore) {
            bestScore = score;
            bestIndex = (int)i;
        }
    }

    return bestIndex;
}

} 
