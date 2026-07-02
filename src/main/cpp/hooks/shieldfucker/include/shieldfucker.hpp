#pragma once

#include "shieldfucker_config.hpp"
#include "shieldfucker_target.hpp"
#include "shieldfucker_math.hpp"
#include "shieldfucker_platform.hpp"

namespace shieldfucker {

bool isSword(const char* itemName);

bool isAxe(const char* itemName);

bool isWeapon(const char* itemName);

int findBestTarget(
    const TargetInfo* targets,
    int targetCount,
    const ShieldFuckerConfig& config,
    const Vec3& playerPos
);

BreakAction tick(
    const Vec3& playerPos,
    float playerYaw,
    float playerPitch,
    const TargetInfo* targets,
    int targetCount,
    const ShieldFuckerConfig& config,
    const char* currentItem,
    int currentSlot
);

} 


namespace shieldfucker {
void trackerReset();
void trackerSetTarget(int entityId);
bool trackerCanBreak(uint64_t delayMs);
bool trackerCanSwitch(uint64_t delayMs);
int trackerGetBreakCount();
} 
