#include "include/shieldfucker.h"
#include <cstring>
#include <cmath>

namespace shieldfucker {

bool isSword(const char* itemName) {
    if (!itemName) return false;
    return std::strstr(itemName, "sword") != nullptr;
}

bool isAxe(const char* itemName) {
    if (!itemName) return false;
    return std::strstr(itemName, "axe") != nullptr;
}

bool isWeapon(const char* itemName) {
    return isSword(itemName) || isAxe(itemName) || std::strstr(itemName, "mace") != nullptr;
}

int findBestTarget(
    const TargetInfo* targets,
    int targetCount,
    const ShieldFuckerConfig& config,
    const Vec3& playerPos)
{
    int bestIndex = -1;
    double bestScore = 999999.0;

    for (int i = 0; i < targetCount; i++) {
        const auto& t = targets[i];

        if (t.entityId < 0) continue;
        if (!t.hasShield || !t.isBlocking) continue;
        if (!config.targetPlayers && t.isPlayer) continue;
        if (!config.targetMonsters && !t.isPlayer) continue;

        double dist = distance(playerPos, {t.x, t.y, t.z});

        if (dist > config.range) continue;
        if (!config.throughWalls && !t.hasLineOfSight) continue;
        if (config.throughWalls && !t.hasLineOfSight && dist > config.wallRange) continue;

        if (dist < bestScore) {
            bestScore = dist;
            bestIndex = i;
        }
    }

    return bestIndex;
}

} // namespace shieldfucker
