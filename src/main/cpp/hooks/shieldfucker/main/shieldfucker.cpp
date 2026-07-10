#include "shieldfucker.hpp"

namespace shieldfucker {

static uint64_t lastBreakTime = 0;
static uint64_t lastSwitchTime = 0;
static int lastTargetId = -1;

void resetState() {
    lastBreakTime = 0;
    lastSwitchTime = 0;
    lastTargetId = -1;
}

BreakAction tick(
    const Vec3& playerPos,
    float playerYaw,
    float playerPitch,
    const TargetInfo* targets,
    int targetCount,
    const ShieldFuckerConfig& config,
    const char* currentItem,
    int currentSlot)
{
    BreakAction action{};

    int idx = findBestTarget(targets, targetCount, config, playerPos);
    if (idx < 0) {
        lastTargetId = -1;
        return action;
    }

    const auto& target = targets[idx];
    uint64_t now = currentTimeMs();

    if (target.entityId != lastTargetId) {
        lastTargetId = target.entityId;
        lastBreakTime = 0;
    }

    Vec3 targetPos = {target.x, target.y + 0.25, target.z};
    action.yaw = calculateYaw(playerPos, targetPos);
    action.pitch = calculatePitch(playerPos, targetPos);
    action.targetId = target.entityId;

    if (config.onlyAxe && !isAxe(currentItem)) {
        if (now - lastSwitchTime >= config.switchDelay) {
            action.shouldSwitch = true;
            action.switchSlot = -1;
            lastSwitchTime = now;
        }
        return action;
    }

    bool isAxeHeld = isAxe(currentItem);

    if (isAxeHeld && now - lastBreakTime >= config.attackDelay) {
        action.shouldBreak = true;
        lastBreakTime = now;
    }

    return action;
}

}
